package com.memora.server.entity;

import com.memora.server.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "health_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private MoodType mostFrequentMood;

    @Builder.Default
    private Integer activityScore = 0;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysisSummary;

    @Builder.Default
    private Boolean isShared = false;

    @Builder.Default
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * 리포트 공유 처리
     */
    public void share() {
        this.isShared = true;
    }

    /**
     * 집계 통계 갱신 — 사용자가 일기를 추가/수정/삭제할 때마다 호출되어
     * mostFrequentMood와 activityScore(총 작성 수)를 최신 값으로 덮어쓴다.
     */
    public void updateStats(MoodType mostFrequentMood, Integer activityScore) {
        this.mostFrequentMood = mostFrequentMood;
        this.activityScore = activityScore;
    }

    /**
     * AI 분석 코멘트 저장
     */
    public void updateAiAnalysisSummary(String summary) {
        this.aiAnalysisSummary = summary;
    }
}