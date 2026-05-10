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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final OpenrouterService openrouterService;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

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

        List<DiarySegment> candidates = segmentRepository.findQuizCandidates(user);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("퀴즈를 만들 사진이 아직 없어요. 사진과 함께 일기를 작성해보세요!");
        }

        DiarySegment selected = candidates.get(random.nextInt(candidates.size()));

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
                Path filePath = Paths.get(uploadDir).toAbsolutePath()
                        .resolve(photoUrl.replace("/uploads/", ""));
                if (Files.exists(filePath)) {
                    base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(filePath));
                    mimeType = Files.probeContentType(filePath);
                    if (mimeType == null) mimeType = "image/jpeg";
                }
            } catch (IOException e) {
                log.warn("퀴즈용 사진 읽기 실패: {}", photoUrl, e);
            }
        }

        // AI 호출
        String aiResponse = openrouterService.chatWithVision(QUIZ_SYSTEM_PROMPT, context.toString(), base64Image, mimeType);

        // JSON 파싱
        try {
            // AI 응답에서 JSON 부분만 추출
            String json = extractJson(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            String question = (String) parsed.get("question");
            List<String> choices = (List<String>) parsed.get("choices");
            String correctAnswer = (String) parsed.get("correctAnswer");

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
                .filter(loc -> !loc.equals(correctAnswer))
                .distinct()
                .toList();

        List<String> choices = new ArrayList<>();
        List<String> defaults = List.of("대전 중구 대흥동", "대전 동구 용전동", "대전 유성구 봉명동");
        int idx = 0;
        for (int i = 0; i < 3; i++) {
            if (i < otherLocations.size()) {
                choices.add(otherLocations.get(i));
            } else if (idx < defaults.size()) {
                if (!defaults.get(idx).equals(correctAnswer)) choices.add(defaults.get(idx));
                idx++;
            }
        }
        choices.add(correctAnswer);
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
}
