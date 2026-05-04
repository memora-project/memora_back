package com.memora.server.controller;

import com.memora.server.dto.diary.DiaryResponse;
import com.memora.server.dto.diary.DiaryUpdateRequest;
import com.memora.server.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 일기 관련 API 엔드포인트
 *
 * 모든 URL은 /api/v1/diaries 로 시작
 * 전부 토큰 필수 (로그인한 사용자만 접근 가능)
 */
@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 일기 작성 시작
     * POST /api/v1/diaries
     *
     * 오늘 날짜로 빈 일기 생성 (이미 있으면 기존 일기 반환)
     * 응답: { diaryId, targetDate, status: "IN_PROGRESS", ... }
     */
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary() {
        Integer userId = getCurrentUserId();
        DiaryResponse response = diaryService.createDiary(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 월별 일기 목록 조회
     * GET /api/v1/diaries?month=2026-04
     *
     * 캘린더 모드에서 한 달치 일기를 보여줄 때 사용
     * 응답: [ { diaryId, targetDate, finalMood, status, ... }, ... ]
     */
    @GetMapping
    public ResponseEntity<List<DiaryResponse>> getDiaries(@RequestParam String month) {
        Integer userId = getCurrentUserId();
        List<DiaryResponse> response = diaryService.getDiariesByMonth(userId, month);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 일기 상세 조회
     * GET /api/v1/diaries/{diaryId}
     *
     * @PathVariable: URL의 {diaryId} 부분을 변수로 받음
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(@PathVariable Integer diaryId) {
        Integer userId = getCurrentUserId();
        DiaryResponse response = diaryService.getDiary(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 최종 일기 수정
     * PATCH /api/v1/diaries/{diaryId}
     *
     * 요청: { finalMood: "GREAT", finalContent: "오늘 하루 좋았다." }
     */
    @PatchMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @PathVariable Integer diaryId,
            @RequestBody DiaryUpdateRequest request) {
        Integer userId = getCurrentUserId();
        DiaryResponse response = diaryService.updateDiary(userId, diaryId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 최종 일기 완료 처리
     * POST /api/v1/diaries/{diaryId}/complete
     *
     * status: IN_PROGRESS → COMPLETED
     */
    @PostMapping("/{diaryId}/complete")
    public ResponseEntity<DiaryResponse> completeDiary(@PathVariable Integer diaryId) {
        Integer userId = getCurrentUserId();
        DiaryResponse response = diaryService.completeDiary(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 삭제
     * DELETE /api/v1/diaries/{diaryId}
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Map<String, String>> deleteDiary(@PathVariable Integer diaryId) {
        Integer userId = getCurrentUserId();
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.ok(Map.of("message", "일기가 삭제되었습니다."));
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
