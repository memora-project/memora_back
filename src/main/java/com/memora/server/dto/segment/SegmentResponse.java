package com.memora.server.dto.segment;

import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.enums.MoodType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 중간 기록 응답 DTO
 *
 * 다중 사진은 photos 배열로 노출. 단일 photoUrl/takenAt/latitude/longitude/locationName은
 * 첫 번째 사진의 mirror로 유지(프론트 호환 + 레거시 데이터 호환).
 */
@Getter
@AllArgsConstructor
public class SegmentResponse {

    private Integer segmentId;
    private Integer stepOrder;
    private MoodType moodSnapshot;

    /** 첫 번째 사진의 url. photos 배열을 받는 클라이언트는 무시 가능. */
    private String photoUrl;
    private OffsetDateTime takenAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;

    private String aiDraft;
    private String userContent;
    private Boolean isEdited;
    private OffsetDateTime createdAt;

    /** 첨부 사진 리스트 (photoOrder 오름차순). */
    private List<PhotoMeta> photos;

    public static SegmentResponse from(DiarySegment segment) {
        // 1) photos가 있으면 그것 그대로 사용
        // 2) photos가 비어있고 단일 photoUrl 컬럼이 채워진 레거시 데이터면, 그것을 photos 1개로 감싸서 응답
        List<PhotoMeta> photoList = new ArrayList<>();
        if (segment.getPhotos() != null && !segment.getPhotos().isEmpty()) {
            segment.getPhotos().forEach(p -> photoList.add(PhotoMeta.from(p)));
        } else if (segment.getPhotoUrl() != null && !segment.getPhotoUrl().isBlank()) {
            photoList.add(new PhotoMeta(
                    null,
                    1,
                    segment.getPhotoUrl(),
                    segment.getTakenAt(),
                    segment.getLatitude(),
                    segment.getLongitude(),
                    segment.getLocationName(),
                    segment.getCreatedAt()
            ));
        }

        return new SegmentResponse(
                segment.getSegmentId(),
                segment.getStepOrder(),
                segment.getMoodSnapshot(),
                segment.getPhotoUrl(),
                segment.getTakenAt(),
                segment.getLatitude(),
                segment.getLongitude(),
                segment.getLocationName(),
                segment.getAiDraft(),
                segment.getUserContent(),
                segment.getIsEdited(),
                segment.getCreatedAt(),
                photoList
        );
    }
}
