package com.memora.server.controller;

import com.memora.server.dto.segment.*;
import com.memora.server.service.SegmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 중간 기록 관련 API 엔드포인트
 *
 * URL: /api/v1/diaries/{diaryId}/segments
 * 일기(diary) 하위 리소스로 설계 (RESTful)
 * 전부 토큰 필수
 */
@RestController
@RequestMapping("/api/v1/diaries/{diaryId}/segments")
@RequiredArgsConstructor
public class SegmentController {

    private final SegmentService segmentService;

    /**
     * 중간 기록 추가
     * POST /api/v1/diaries/{diaryId}/segments
     *
     * 요청: { moodSnapshot: "GREAT", photoUrl: "...", ... }
     * stepOrder는 자동 부여
     */
    @PostMapping
    public ResponseEntity<SegmentResponse> createSegment(
            @PathVariable Integer diaryId,
            @Valid @RequestBody SegmentCreateRequest request) {
        Integer userId = getCurrentUserId();
        SegmentResponse response = segmentService.createSegment(userId, diaryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 중간 기록 목록 조회
     * GET /api/v1/diaries/{diaryId}/segments
     *
     * stepOrder 순서대로 반환
     */
    @GetMapping
    public ResponseEntity<List<SegmentResponse>> getSegments(@PathVariable Integer diaryId) {
        Integer userId = getCurrentUserId();
        List<SegmentResponse> response = segmentService.getSegments(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 중간 기록 수정
     * PATCH /api/v1/diaries/{diaryId}/segments/{segmentId}
     *
     * 요청: { moodSnapshot: "CALM", userContent: "수정된 내용" }
     */
    @PatchMapping("/{segmentId}")
    public ResponseEntity<SegmentResponse> updateSegment(
            @PathVariable Integer diaryId,
            @PathVariable Integer segmentId,
            @RequestBody SegmentUpdateRequest request) {
        Integer userId = getCurrentUserId();
        SegmentResponse response = segmentService.updateSegment(userId, diaryId, segmentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 중간 기록 삭제
     * DELETE /api/v1/diaries/{diaryId}/segments/{segmentId}
     *
     * 삭제 후 남은 세그먼트 순서 자동 재정렬
     */
    @DeleteMapping("/{segmentId}")
    public ResponseEntity<Map<String, String>> deleteSegment(
            @PathVariable Integer diaryId,
            @PathVariable Integer segmentId) {
        Integer userId = getCurrentUserId();
        segmentService.deleteSegment(userId, diaryId, segmentId);
        return ResponseEntity.ok(Map.of("message", "중간 기록이 삭제되었습니다."));
    }

    /**
     * 중간 기록 순서 변경
     * PATCH /api/v1/diaries/{diaryId}/segments/order
     *
     * 요청: { segmentIds: [3, 1, 2] }
     * → 세그먼트3이 1번, 세그먼트1이 2번, 세그먼트2가 3번
     */
    @PatchMapping("/order")
    public ResponseEntity<List<SegmentResponse>> reorderSegments(
            @PathVariable Integer diaryId,
            @RequestBody SegmentOrderRequest request) {
        Integer userId = getCurrentUserId();
        List<SegmentResponse> response = segmentService.reorderSegments(userId, diaryId, request.getSegmentIds());
        return ResponseEntity.ok(response);
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
