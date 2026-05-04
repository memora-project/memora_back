package com.memora.server.dto.segment;

import com.memora.server.entity.SegmentPhoto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 사진 한 장의 메타데이터 — segment 생성 요청과 응답 양쪽에서 공용으로 사용.
 *
 * 요청 시:  photoUrl(필수, /files/images 업로드 후 받은 url) + EXIF 메타(선택)
 * 응답 시:  photoId/photoOrder/createdAt까지 채워서 반환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhotoMeta {

    private Integer photoId;
    private Integer photoOrder;
    private String photoUrl;
    private OffsetDateTime takenAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;
    private OffsetDateTime createdAt;

    public static PhotoMeta from(SegmentPhoto photo) {
        return new PhotoMeta(
                photo.getPhotoId(),
                photo.getPhotoOrder(),
                photo.getPhotoUrl(),
                photo.getTakenAt(),
                photo.getLatitude(),
                photo.getLongitude(),
                photo.getLocationName(),
                photo.getCreatedAt()
        );
    }
}
