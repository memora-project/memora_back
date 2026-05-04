package com.memora.server.controller;

import com.memora.server.dto.report.ReportResponse;
import com.memora.server.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 리포트 관련 API 엔드포인트
 *
 * 주간/월간 기분 통계 + 리포트 공유
 * 전부 토큰 필수
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 주간 리포트 조회
     * GET /api/v1/reports/weekly
     *
     * 이번 주 월~일 기준 기분 통계
     * 응답: { moodDistribution: { GREAT: 3, CALM: 2, ... }, mostFrequentMood, activityScore, ... }
     */
    @GetMapping("/weekly")
    public ResponseEntity<ReportResponse> getWeeklyReport() {
        Integer userId = getCurrentUserId();
        ReportResponse response = reportService.getWeeklyReport(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 월간 리포트 조회
     * GET /api/v1/reports/monthly
     *
     * 이번 달 기준 기분 통계
     */
    @GetMapping("/monthly")
    public ResponseEntity<ReportResponse> getMonthlyReport() {
        Integer userId = getCurrentUserId();
        ReportResponse response = reportService.getMonthlyReport(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 리포트 공유 처리
     * POST /api/v1/reports/{reportId}/share
     *
     * 비상연락처(가족)에게 리포트 공유
     * (실제 전송은 추후 구현)
     */
    @PostMapping("/{reportId}/share")
    public ResponseEntity<ReportResponse> shareReport(@PathVariable Integer reportId) {
        Integer userId = getCurrentUserId();
        ReportResponse response = reportService.shareReport(userId, reportId);
        return ResponseEntity.ok(response);
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
