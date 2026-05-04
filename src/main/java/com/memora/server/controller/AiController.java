package com.memora.server.controller;

import com.memora.server.dto.diary.DiaryResponse;
import com.memora.server.dto.segment.SegmentResponse;
import com.memora.server.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 일기 초안 생성 엔드포인트.
 *
 * 모든 URL은 /api/v1/diaries 하위. 토큰 필수.
 * 클라이언트(앱)에는 OpenRouter 키도, 시스템 프롬프트도 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 단일 세그먼트(중간 기록)에 대한 AI 초안 생성/재생성
     * POST /api/v1/diaries/{diaryId}/segments/{segmentId}/ai-draft
     *
     * 응답: SegmentResponse — aiDraft 필드가 채워진 상태로 반환.
     */
    @PostMapping("/{diaryId}/segments/{segmentId}/ai-draft")
    public ResponseEntity<SegmentResponse> generateSegmentDraft(
            @PathVariable Long diaryId,
            @PathVariable Long segmentId) {
        Long userId = currentUserId();
        SegmentResponse response = aiService.generateSegmentDraft(userId, diaryId, segmentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 하루의 final diary 초안 생성/재생성
     * POST /api/v1/diaries/{diaryId}/ai-draft
     *
     * 그날의 모든 segments를 종합해 final 일기 본문을 생성한다.
     * 응답: DiaryResponse — aiDraft 필드가 채워진 상태로 반환.
     */
    @PostMapping("/{diaryId}/ai-draft")
    public ResponseEntity<DiaryResponse> generateDiaryDraft(@PathVariable Long diaryId) {
        Long userId = currentUserId();
        DiaryResponse response = aiService.generateDiaryDraft(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
