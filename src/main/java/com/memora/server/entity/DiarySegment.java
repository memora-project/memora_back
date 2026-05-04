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
    private Diary diary;

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
    private String aiDraft;

    @Column(columnDefinition = "TEXT")
    private String userContent;

    @Builder.Default
    private Boolean isEdited = false;

    @Builder.Default
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * 중간 기록 수정
     * 기분, 사용자 내용 업데이트
     */
    public void update(MoodType moodSnapshot, String userContent) {
        if (moodSnapshot != null) this.moodSnapshot = moodSnapshot;
        if (userContent != null) {
            this.userContent = userContent;
            this.isEdited = true;
        }
    }

    /**
     * AI 초안 저장 (재생성 시 덮어쓰기)
     */
    public void updateAiDraft(String aiDraft) {
        this.aiDraft = aiDraft;
    }

    /**
     * 순서 변경
     */
    public void updateStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }
}
