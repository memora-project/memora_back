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
    private Long reportId;

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
}