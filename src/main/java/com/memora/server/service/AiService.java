package com.memora.server.service;

import com.memora.server.dto.diary.DiaryResponse;
import com.memora.server.dto.segment.SegmentResponse;
import com.memora.server.entity.Diary;
import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.User;
import com.memora.server.entity.enums.GenderType;
import com.memora.server.entity.enums.MoodType;
import com.memora.server.repository.DiaryRepository;
import com.memora.server.repository.DiarySegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * AI 일기 초안 생성 비즈니스 로직.
 *
 * 책임:
 *  - 본인 소유 검증 (다른 사용자 일기/세그먼트로 AI 호출 차단)
 *  - 호출 빈도 제한
 *  - DB 데이터 → 프롬프트 변환
 *  - OpenrouterService로 호출 위임
 *  - 결과를 entity의 aiDraft 컬럼에 저장
 *
 * 시스템 프롬프트는 서버에서만 관리한다. 클라이언트로 절대 노출 X.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiService {

    private final DiaryRepository diaryRepository;
    private final DiarySegmentRepository segmentRepository;
    private final OpenrouterService openrouterService;
    private final AiRateLimiter rateLimiter;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 단일 중간 기록(Segment)용 프롬프트 템플릿. 한 번의 기록 → 3~4문장 초안.
     * {persona} = "70대 김순자 할머니" 같은 호출 컨텍스트.
     * {honorific} = "할머니"/"할아버지"/"어르신" — 페르소나 문장 안에서 재참조용.
     */
    private static final String SEGMENT_SYSTEM_PROMPT_TEMPLATE = String.join(" ",
            "너는 {persona}를 모시는 10대 다정하고 섬세한 손주야.",
            "말투는 경어체를 사용하며, {honorific}의 하루를 응원하는 따뜻한 분위기여야 해.",
            "입력으로 받은 [기분, 시간, 장소] 정보와 사용자가 남긴 [한 줄 메모]를 조합해",
            "3~4문장의 감성적인 일기 초안을 작성해줘.",
            "메모가 없다면 사진 정보를 기반으로 따뜻한 상상을 더해줘.",
            "결과는 따로 머리말이나 인용부호 없이 일기 본문만 출력해."
    );

    /** 리포트 AI 분석 코멘트용 프롬프트. 기분 분포 데이터 → 1~2문장 따뜻한 코멘트. */
    private static final String REPORT_SYSTEM_PROMPT = String.join(" ",
            "너는 70대 어르신을 모시는 10대 다정하고 섬세한 손주야.",
            "말투는 경어체를 사용하며, 어르신의 감정 변화를 따뜻하게 읽어주는 분위기여야 해.",
            "입력으로 받은 [기간, 기분 분포, 가장 빈번한 기분, 총 작성 수] 데이터를 바탕으로",
            "1~2문장의 따뜻하고 격려하는 분석 코멘트를 작성해줘.",
            "결과는 따로 머리말이나 인용부호 없이 코멘트 본문만 출력해."
    );

    /** 하루 final diary용 프롬프트 템플릿. 여러 segment를 묶어 8~12문장 일기. */
    private static final String FINAL_SYSTEM_PROMPT_TEMPLATE = String.join(" ",
            "너는 {persona}를 모시는 10대 다정하고 섬세한 손주야.",
            "말투는 경어체를 사용하며, {honorific}의 하루를 따뜻하게 정리해주는 분위기여야 해.",
            "입력으로 받은 여러 개의 중간 기록들을 시간 순서대로 자연스럽게 엮어서",
            "하루를 돌아보는 8~12문장 분량의 일기를 작성해줘.",
            "각 중간 기록의 한 줄 메모(있는 경우)와 AI 초안을 모두 참고해.",
            "결과는 따로 머리말이나 인용부호 없이 일기 본문만 출력해."
    );

    /**
     * 단일 세그먼트(중간 기록) AI 초안 생성/재생성.
     *
     * 흐름: ownership 검증 → rate limit 체크 → 프롬프트 조립 → OpenRouter 호출
     *      → segment.aiDraft 저장 → SegmentResponse 반환
     */
    @Transactional
    public SegmentResponse generateSegmentDraft(Integer userId, Integer diaryId, Integer segmentId) {
        DiarySegment segment = findSegmentByIdAndUser(segmentId, diaryId, userId);
        rateLimiter.check(userId);

        User user = segment.getDiary().getUser();
        String systemPrompt = resolvePersona(SEGMENT_SYSTEM_PROMPT_TEMPLATE, user);
        String userPrompt = buildSegmentUserPrompt(segment);

        // 사진이 있으면 Vision으로 호출, 없으면 텍스트만
        String photoUrl = getSegmentPhotoUrl(segment);
        String base64Image = null;
        String mimeType = null;

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
                log.warn("사진 읽기 실패, 텍스트만으로 진행: {}", photoUrl, e);
            }
        }

        String draft = openrouterService.chatWithVision(systemPrompt, userPrompt, base64Image, mimeType);

        segment.updateAiDraft(draft);
        return SegmentResponse.from(segment);
    }

    /**
     * 하루 final diary 초안 생성/재생성.
     *
     * 흐름: ownership 검증 → segments 조회 → rate limit 체크 → 종합 프롬프트 조립
     *      → OpenRouter 호출 → diary.aiDraft 저장 → DiaryResponse 반환
     */
    @Transactional
    public DiaryResponse generateDiaryDraft(Integer userId, Integer diaryId) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);
        List<DiarySegment> segments = segmentRepository.findByDiaryOrderByStepOrderAsc(diary);

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("중간 기록이 없어서 final 일기를 만들 수 없습니다.");
        }

        rateLimiter.check(userId);

        String systemPrompt = resolvePersona(FINAL_SYSTEM_PROMPT_TEMPLATE, diary.getUser());
        String userPrompt = buildDiaryUserPrompt(segments);
        String draft = openrouterService.chat(systemPrompt, userPrompt);

        diary.updateAiDraft(draft);
        return DiaryResponse.from(diary);
    }

    // ------------------------------------------------------------- persona resolver

    /**
     * 사용자 정보로 시스템 프롬프트의 {persona} / {honorific} 자리표시자를 채운다.
     *
     * 예) name="김순자", gender=FEMALE, birthDate=1955-03-15
     *     → persona="70대 김순자 할머니", honorific="할머니"
     *
     * 누락 처리:
     *  - birthDate null → 나이대 생략
     *  - name null/blank → 이름 생략
     *  - gender null/OTHER → "어르신" (중성)
     */
    private String resolvePersona(String template, User user) {
        String honorific = honorificFor(user != null ? user.getGender() : null);
        String persona = buildPersona(user, honorific);
        return template
                .replace("{persona}", persona)
                .replace("{honorific}", honorific);
    }

    private String buildPersona(User user, String honorific) {
        if (user == null) return honorific;

        StringBuilder sb = new StringBuilder();
        int decade = ageDecade(user.getBirthDate());
        if (decade > 0) {
            sb.append(decade).append("대 ");
        }
        String name = user.getName();
        if (name != null && !name.isBlank()) {
            sb.append(name.trim()).append(" ");
        }
        sb.append(honorific);
        return sb.toString();
    }

    private String honorificFor(GenderType g) {
        if (g == null) return "어르신";
        return switch (g) {
            case FEMALE -> "할머니";
            case MALE -> "할아버지";
            default -> "어르신"; // OTHER 등
        };
    }

    private int ageDecade(LocalDate birthDate) {
        if (birthDate == null) return 0;
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 0) return 0;
        return (age / 10) * 10;
    }

    // ------------------------------------------------------------- prompt builders

    private String buildSegmentUserPrompt(DiarySegment s) {
        StringBuilder sb = new StringBuilder();
        sb.append("기분: ").append(moodLabel(s.getMoodSnapshot())).append('\n');
        sb.append("시간: ")
                .append(s.getTakenAt() != null
                        ? s.getTakenAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        : "알 수 없음")
                .append('\n');
        sb.append("장소: ")
                .append(s.getLocationName() != null && !s.getLocationName().isBlank()
                        ? s.getLocationName()
                        : "위치 정보 없음")
                .append('\n');
        String memo = s.getUserContent();
        sb.append("한 줄 메모: ")
                .append(memo != null && !memo.isBlank() ? memo : "(메모 없음)");
        return sb.toString();
    }

    private String buildDiaryUserPrompt(List<DiarySegment> segments) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래는 어르신이 하루 동안 남긴 ")
                .append(segments.size())
                .append("개의 중간 기록입니다. 시간 순서대로 자연스럽게 하나의 일기로 엮어주세요.\n\n");
        for (int i = 0; i < segments.size(); i++) {
            DiarySegment s = segments.get(i);
            sb.append("[기록 ").append(i + 1).append("]\n");
            sb.append("기분: ").append(moodLabel(s.getMoodSnapshot())).append('\n');
            if (s.getTakenAt() != null) {
                sb.append("시간: ")
                        .append(s.getTakenAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                        .append('\n');
            }
            if (s.getLocationName() != null && !s.getLocationName().isBlank()) {
                sb.append("장소: ").append(s.getLocationName()).append('\n');
            }
            String memo = s.getUserContent();
            if (memo != null && !memo.isBlank()) {
                sb.append("한 줄 메모: ").append(memo).append('\n');
            }
            String draft = s.getAiDraft();
            if (draft != null && !draft.isBlank()) {
                sb.append("AI 초안: ").append(draft).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String moodLabel(MoodType m) {
        if (m == null) return "선택 안 함";
        return switch (m) {
            case GREAT -> "최고에요";
            case CALM -> "평온해요";
            case UNKNOWN -> "저도 모르겠어요";
            case SAD -> "슬퍼요";
            case ANGRY -> "화나요";
            case PAIN -> "몸이 안 좋아요";
        };
    }

    // ------------------------------------------------------------- photo helper

    /**
     * 세그먼트의 첫 번째 사진 URL 가져오기 (없으면 null)
     */
    private String getSegmentPhotoUrl(DiarySegment segment) {
        if (segment.getPhotos() != null && !segment.getPhotos().isEmpty()) {
            return segment.getPhotos().get(0).getPhotoUrl();
        }
        return segment.getPhotoUrl();
    }

    // ------------------------------------------------------------- report AI comment

    /**
     * 리포트 AI 분석 코멘트 생성.
     *
     * @param periodLabel  "주간" 또는 "월간"
     * @param distribution 기분 분포 (예: {GREAT: 3, CALM: 2, SAD: 1})
     * @param mostFrequentMood 가장 빈번한 기분
     * @param totalEntries 총 작성 수
     * @return 1~2문장 코멘트
     */
    public String generateReportComment(String periodLabel, Map<String, Long> distribution,
                                        MoodType mostFrequentMood, int totalEntries) {
        StringBuilder sb = new StringBuilder();
        sb.append("기간: ").append(periodLabel).append(" 리포트\n");
        sb.append("총 기록 수: ").append(totalEntries).append("개\n");
        sb.append("가장 빈번한 기분: ").append(mostFrequentMood != null ? moodLabel(mostFrequentMood) : "없음").append("\n");
        sb.append("기분 분포:\n");
        for (var entry : distribution.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append("  - ").append(moodLabel(MoodType.valueOf(entry.getKey())))
                        .append(": ").append(entry.getValue()).append("회\n");
            }
        }

        return openrouterService.chat(REPORT_SYSTEM_PROMPT, sb.toString());
    }

    // ------------------------------------------------------------- ownership helpers

    private Diary findDiaryByIdAndUser(Integer diaryId, Integer userId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));
        if (!diary.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return diary;
    }

    private DiarySegment findSegmentByIdAndUser(Integer segmentId, Integer diaryId, Integer userId) {
        DiarySegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("중간 기록을 찾을 수 없습니다."));
        if (!segment.getDiary().getDiaryId().equals(diaryId)) {
            throw new IllegalArgumentException("해당 일기의 중간 기록이 아닙니다.");
        }
        if (!segment.getDiary().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return segment;
    }
}
