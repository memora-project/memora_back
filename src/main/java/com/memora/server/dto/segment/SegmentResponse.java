package com.memora.server.dto.segment;

import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.enums.MoodType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 중간 기록 응답 DTO
 *
 * 응답 예시:
 * {
 *     "segmentId": 1,
 *     "stepOrder": 1,
 *     "moodSnapshot": "GREAT",
 *     "photoUrl": "https://...",
 *     "takenAt": "2026-04-12T10:30:00+09:00",
 *     "latitude": 36.3504,
 *     "longitude": 127.3845,
 *     "locationName": "대전 유성구",
 *     "aiDraft": "AI가 작성한 초안",
 *     "userContent": "사용자가 수정한 내용",
 *     "isEdited": false,
 *     "createdAt": "..."
 * }
 */
@Getter
@AllArgsConstructor
public class SegmentResponse {

    private Long segmentId;
    private Integer stepOrder;
    private MoodType moodSnapshot;
    private String photoUrl;
    private OffsetDateTime takenAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;
    private String aiDraft;
    private String userContent;
    private Boolean isEdited;
    private OffsetDateTime createdAt;

    public static SegmentResponse from(DiarySegment segment) {
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
                segment.getCreatedAt()
        );
    }
}
