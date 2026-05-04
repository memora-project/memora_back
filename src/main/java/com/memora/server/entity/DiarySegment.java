package com.memora.server.entity;

import com.memora.server.entity.enums.MoodType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diary_segments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DiarySegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer segmentId;

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

    /**
     * 다중 사진 첨부 — 1:N. orphanRemoval=true로 segment 삭제 시 사진도 같이 정리.
     * 단일 photoUrl 컬럼은 첫 번째 사진의 mirror로 호환 유지(레거시 컨슈머 보호).
     */
    @OneToMany(mappedBy = "segment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("photoOrder ASC")
    @Builder.Default
    private List<SegmentPhoto> photos = new ArrayList<>();

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

    /**
     * 사진 추가 — segment.photos에 양방향으로 연결 + 첫 번째 사진의 메타를
     * 단일 컬럼들에 mirror해서 레거시 코드 경로(getPhotoUrl 등)와 호환.
     */
    public void addPhoto(SegmentPhoto photo) {
        this.photos.add(photo);
        if (this.photos.size() == 1) {
            this.photoUrl = photo.getPhotoUrl();
            this.takenAt = photo.getTakenAt();
            this.latitude = photo.getLatitude();
            this.longitude = photo.getLongitude();
            this.locationName = photo.getLocationName();
        }
    }
}
