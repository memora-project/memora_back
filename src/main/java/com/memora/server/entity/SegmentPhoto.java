package com.memora.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 중간 기록(DiarySegment)에 첨부된 사진.
 * 한 segment 안에 여러 장 첨부 가능 → photoOrder로 순서를 보존한다.
 *
 * 사진별로 EXIF의 takenAt/lat/lng를 따로 들고 있는 이유:
 * 한 번의 segment 작성에서도 여러 장의 사진이 서로 다른 시점·위치에서 찍혔을 수 있음.
 * 동선 분석/회상 보조에 그 차이가 의미 있게 쓰임.
 */
@Entity
@Table(name = "segment_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SegmentPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer photoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private DiarySegment segment;

    @Column(nullable = false, length = 500)
    private String photoUrl;

    /** 한 segment 내 사진 순서. 1부터 시작. */
    @Column(nullable = false)
    private Integer photoOrder;

    private OffsetDateTime takenAt;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(length = 300)
    private String locationName;

    @Builder.Default
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
