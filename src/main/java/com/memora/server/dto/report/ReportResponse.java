package com.memora.server.dto.report;

import com.memora.server.entity.HealthReport;
import com.memora.server.entity.enums.MoodType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 리포트 응답 DTO
 *
 * 응답 예시:
 * {
 *     "reportId": 1,
 *     "startDate": "2026-04-06",
 *     "endDate": "2026-04-12",
 *     "mostFrequentMood": "GREAT",
 *     "activityScore": 5,
 *     "aiAnalysisSummary": "이번 주는 평온한 날이 많으셨네요!",
 *     "isShared": false,
 *     "moodDistribution": { "GREAT": 3, "CALM": 2, "SAD": 1, "ANGRY": 1 },
 *     "totalDiaries": 7,
 *     "createdAt": "..."
 * }
 */
@Getter
@AllArgsConstructor
public class ReportResponse {

    private Long reportId;
    private LocalDate startDate;
    private LocalDate endDate;
    private MoodType mostFrequentMood;
    private Integer activityScore;
    private String aiAnalysisSummary;
    private Boolean isShared;
    private Map<String, Long> moodDistribution;
    private Integer totalDiaries;
    private OffsetDateTime createdAt;

    public static ReportResponse from(HealthReport report, Map<String, Long> moodDistribution, int totalDiaries) {
        return new ReportResponse(
                report.getReportId(),
                report.getStartDate(),
                report.getEndDate(),
                report.getMostFrequentMood(),
                report.getActivityScore(),
                report.getAiAnalysisSummary(),
                report.getIsShared(),
                moodDistribution,
                totalDiaries,
                report.getCreatedAt()
        );
    }
}
