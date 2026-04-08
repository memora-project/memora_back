package com.memora.server.entity;

import com.memora.server.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "diary_segments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiarySegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long segmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary; // 어떤 일기의 조각인지 연결

    @Column(nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoodType moodSnapshot;

    @Column(length = 500)
    private String photoUrl;

    private OffsetDateTime takenAt;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(length = 300)
    private String locationName;

    @Column(columnDefinition = "TEXT")
    private String aiComment;
}