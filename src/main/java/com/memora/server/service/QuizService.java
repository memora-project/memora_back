package com.memora.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memora.server.dto.quiz.QuizAnswerRequest;
import com.memora.server.dto.quiz.QuizResponse;
import com.memora.server.dto.quiz.QuizResultResponse;
import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.User;
import com.memora.server.repository.DiarySegmentRepository;
import com.memora.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI 기반 퀴즈 서비스
 *
 * 사진 Vision 분석 + 일기 기록을 조합하여
 * AI가 다양한 유형의 퀴즈를 생성한다.
 * 알츠하이머 예방 목적의 기억력 훈련.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final DiarySegmentRepository segmentRepository;
    private final UserRepository userRepository;
    private final AiClient aiClient;
    private final FileService fileService;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 사용자별 최근 출제된 "세그먼트+문제 유형" 기록. 사진이 하나뿐이어도
    // 날짜/기분/장소 문제를 순환해 같은 문제가 연속으로 나오지 않게 한다.
    private final java.util.concurrent.ConcurrentHashMap<Integer, java.util.Deque<String>> recentQuizzes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Integer, QuizType> lastQuizTypes = new java.util.concurrent.ConcurrentHashMap<>();

    private enum QuizType {
        DATE,
        MOOD,
        LOCATION
    }

    private record QuizCandidate(DiarySegment segment, QuizType type) {
        String key() {
            return segment.getSegmentId() + ":" + type.name();
        }
    }

    private static final String QUIZ_SYSTEM_PROMPT = String.join(" ",
            "너는 70대 어르신의 기억력 훈련을 도와주는 퀴즈 출제자야.",
            "사진과 일기 기록을 분석해서 장소/위치 기반 4지선다 퀴즈 1개를 만들어줘.",
            "퀴즈는 반드시 장소나 위치에 대한 질문이어야 해.",
            "예시: '이 사진은 어디에서 찍은 걸까요?', '이 날 방문한 곳은 어디일까요?', '이 풍경은 어느 동네일까요?'",
            "어르신이 실제로 방문한 장소를 기반으로 따뜻한 톤으로 출제해.",
            "보기 4개는 모두 실제 존재하는 대전 지역 장소여야 해.",
            "반드시 아래 JSON 형식으로만 응답해. 다른 텍스트는 넣지 마.",
            "{",
            "  \"question\": \"퀴즈 질문\",",
            "  \"choices\": [\"보기1\", \"보기2\", \"보기3\", \"보기4\"],",
            "  \"correctAnswer\": \"정답 (choices 중 하나와 정확히 일치)\"",
            "}"
    );

    /**
     * AI 기반 퀴즈 생성
     *
     * 1. 사진+위치 있는 세그먼트 중 랜덤 1개 선택
     * 2. 사진을 Base64로 변환 + 기록 데이터 조합
     * 3. AI에 Vision + 텍스트로 퀴즈 생성 요청
     */
    public QuizResponse generateQuiz(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 완료된 일기(COMPLETED)의 세그먼트만 퀴즈 대상
        List<DiarySegment> candidates = segmentRepository.findQuizCandidates(user).stream()
                .filter(s -> s.getDiary().getStatus() == com.memora.server.entity.enums.DiaryStatus.COMPLETED)
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("최종 일기를 작성해야 퀴즈를 만들 수 있어요!");
        }

        List<QuizCandidate> quizCandidates = candidates.stream()
                .flatMap(segment -> quizTypesFor(segment).stream()
                        .map(type -> new QuizCandidate(segment, type)))
                .toList();

        // 최근 출제된 정확한 문제 조합 제외
        java.util.Deque<String> recent = recentQuizzes.computeIfAbsent(userId, k -> new java.util.ArrayDeque<>());
        List<QuizCandidate> fresh = quizCandidates.stream()
                .filter(candidate -> !recent.contains(candidate.key()))
                .toList();

        // 모든 조합을 한 번씩 출제한 뒤에만 새 순환을 시작한다.
        if (fresh.isEmpty()) {
            recent.clear();
            fresh = quizCandidates;
        }

        // 가능한 동안에는 직전 문제와 다른 유형을 우선해 체감상 같은 문제가
        // 연속해서 나오는 것도 방지한다.
        QuizType lastType = lastQuizTypes.get(userId);
        List<QuizCandidate> differentType = fresh.stream()
                .filter(candidate -> candidate.type() != lastType)
                .toList();
        if (!differentType.isEmpty()) {
            fresh = differentType;
        }

        QuizCandidate quizCandidate = fresh.get(random.nextInt(fresh.size()));
        DiarySegment selected = quizCandidate.segment();

        recent.addLast(quizCandidate.key());
        lastQuizTypes.put(userId, quizCandidate.type());
        while (recent.size() > quizCandidates.size()) recent.pollFirst();

        if (quizCandidate.type() == QuizType.DATE) {
            return dateQuiz(selected);
        }
        if (quizCandidate.type() == QuizType.MOOD) {
            return moodQuiz(selected);
        }

        // 기록 데이터 조합
        StringBuilder context = new StringBuilder();
        context.append("날짜: ").append(selected.getDiary().getTargetDate()).append("\n");
        if (selected.getLocationName() != null) {
            context.append("장소: ").append(selected.getLocationName()).append("\n");
        }
        if (selected.getMoodSnapshot() != null) {
            context.append("기분: ").append(selected.getMoodSnapshot().name()).append("\n");
        }
        if (selected.getUserContent() != null && !selected.getUserContent().isBlank()) {
            context.append("한줄 메모: ").append(selected.getUserContent()).append("\n");
        }
        if (selected.getAiDraft() != null && !selected.getAiDraft().isBlank()) {
            context.append("일기 내용: ").append(selected.getAiDraft()).append("\n");
        }
        context.append("\n이 정보와 사진을 보고 4지선다 퀴즈 1개를 만들어줘.");

        // 사진 Base64 변환
        String base64Image = null;
        String mimeType = null;
        String photoUrl = selected.getPhotoUrl();

        if (photoUrl != null) {
            try {
                var image = fileService.readImage(photoUrl, userId);
                base64Image = Base64.getEncoder().encodeToString(image.bytes());
                mimeType = image.contentType();
            } catch (IOException e) {
                log.warn("퀴즈용 사진 읽기 실패: {}", photoUrl, e);
            }
        }

        // AI 호출
        String aiResponse = aiClient.chatWithVision(QUIZ_SYSTEM_PROMPT, context.toString(), base64Image, mimeType);

        // JSON 파싱
        try {
            // AI 응답에서 JSON 부분만 추출
            String json = extractJson(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            String question = (String) parsed.get("question");
            List<String> choices = (List<String>) parsed.get("choices");
            String correctAnswer = (String) parsed.get("correctAnswer");
            validateQuiz(question, choices, correctAnswer);

            return new QuizResponse(
                    selected.getSegmentId(),
                    selected.getPhotoUrl(),
                    question,
                    choices,
                    correctAnswer,
                    selected.getDiary().getTargetDate().toString()
            );
        } catch (Exception e) {
            log.error("AI 퀴즈 파싱 실패, 기본 퀴즈로 대체: {}", aiResponse, e);
            // 파싱 실패 시 기본 위치 퀴즈로 폴백
            return fallbackQuiz(selected, candidates);
        }
    }

    private List<QuizType> quizTypesFor(DiarySegment segment) {
        List<QuizType> types = new ArrayList<>(List.of(QuizType.DATE, QuizType.MOOD));
        if (segment.getLocationName() != null && !segment.getLocationName().isBlank()) {
            types.add(QuizType.LOCATION);
        }
        return types;
    }

    private void validateQuiz(String question, List<String> choices, String correctAnswer) {
        if (question == null || question.isBlank()
                || choices == null || choices.size() != 4
                || choices.stream().anyMatch(choice -> choice == null || choice.isBlank())
                || choices.stream().distinct().count() != 4
                || correctAnswer == null || !choices.contains(correctAnswer)) {
            throw new IllegalArgumentException("AI가 유효하지 않은 퀴즈를 반환했습니다.");
        }
    }

    /**
     * 정답 확인
     */
    public QuizResultResponse checkAnswer(Integer userId, QuizAnswerRequest request) {
        DiarySegment segment = segmentRepository.findById(request.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("퀴즈를 찾을 수 없습니다."));

        if (!segment.getDiary().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        boolean isCorrect = request.getAnswer().equals(request.getCorrectAnswer());

        String message = isCorrect
                ? "정답이에요! 기억력이 대단하세요!"
                : "아쉽지만 정답은 " + request.getCorrectAnswer() + "이에요. 다음엔 맞출 수 있을 거예요!";

        return new QuizResultResponse(isCorrect, message, request.getCorrectAnswer());
    }

    /**
     * AI 응답에서 JSON 부분만 추출
     */
    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * AI 퀴즈 생성 실패 시 기본 위치 퀴즈로 폴백
     */
    private QuizResponse fallbackQuiz(DiarySegment selected, List<DiarySegment> candidates) {
        String correctAnswer = selected.getLocationName();
        List<String> otherLocations = candidates.stream()
                .map(DiarySegment::getLocationName)
                .filter(Objects::nonNull)
                .filter(loc -> !loc.isBlank())
                .filter(loc -> !loc.equals(correctAnswer))
                .distinct()
                .toList();

        LinkedHashSet<String> uniqueChoices = new LinkedHashSet<>();
        uniqueChoices.add(correctAnswer);
        uniqueChoices.addAll(otherLocations);
        uniqueChoices.addAll(List.of(
                "대전 중구 대흥동",
                "대전 동구 용전동",
                "대전 유성구 봉명동",
                "대전 서구 둔산동",
                "대전 대덕구 송촌동"
        ));
        List<String> choices = new ArrayList<>(uniqueChoices.stream().limit(4).toList());
        Collections.shuffle(choices);

        return new QuizResponse(
                selected.getSegmentId(),
                selected.getPhotoUrl(),
                "이 사진은 어디에서 찍은 걸까요?",
                choices,
                correctAnswer,
                selected.getDiary().getTargetDate().toString()
        );
    }

    private QuizResponse dateQuiz(DiarySegment selected) {
        LocalDate date = selected.getDiary().getTargetDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일");
        String correctAnswer = date.format(formatter);
        List<String> choices = new ArrayList<>(List.of(
                date.minusDays(2).format(formatter),
                date.minusDays(1).format(formatter),
                correctAnswer,
                date.plusDays(1).format(formatter)
        ));
        Collections.shuffle(choices);
        return new QuizResponse(
                selected.getSegmentId(),
                selected.getPhotoUrl(),
                "이 사진은 언제 찍은 걸까요?",
                choices,
                correctAnswer,
                date.toString()
        );
    }

    private QuizResponse moodQuiz(DiarySegment selected) {
        String correctAnswer = moodLabel(selected.getMoodSnapshot());
        List<String> choices = new ArrayList<>(List.of("최고에요", "평온해요", "슬퍼요", "몸이 안 좋아요"));
        if (!choices.contains(correctAnswer)) {
            choices.set(choices.size() - 1, correctAnswer);
        }
        Collections.shuffle(choices);
        return new QuizResponse(
                selected.getSegmentId(),
                selected.getPhotoUrl(),
                "이 사진을 기록할 때 기분은 어땠을까요?",
                choices,
                correctAnswer,
                selected.getDiary().getTargetDate().toString()
        );
    }

    private String moodLabel(com.memora.server.entity.enums.MoodType mood) {
        if (mood == null) return "저도 모르겠어요";
        return switch (mood) {
            case GREAT -> "최고에요";
            case CALM -> "평온해요";
            case UNKNOWN -> "저도 모르겠어요";
            case SAD -> "슬퍼요";
            case ANGRY -> "화나요";
            case PAIN -> "몸이 안 좋아요";
        };
    }
}
