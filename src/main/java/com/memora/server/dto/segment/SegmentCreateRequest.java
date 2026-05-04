package com.memora.server.dto.segment;

import com.memora.server.entity.enums.MoodType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 중간 기록 생성 요청 DTO
 *
 * 사진 첨부:
 *   - photos: 다중 사진 (권장). 각 항목에 photoUrl + EXIF 메타.
 *   - 단일 photoUrl/takenAt/...: photos가 비어있을 때만 사용되는 레거시 경로.
 *     둘 다 보내면 photos가 우선.
 */
@Getter
@NoArgsConstructor
public class SegmentCreateRequest {

    @NotNull(message = "기분을 선택해주세요.")
    private MoodType moodSnapshot;

    /** 다중 사진. 비어있거나 null이면 단일 photoUrl 경로로 폴백. */
    private List<PhotoMeta> photos;

    // ── 레거시 단일 사진 필드 (photos가 비어있을 때만 사용) ─────────────
    private String photoUrl;
    private OffsetDateTime takenAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;

    /**
     * 한 줄 메모. AI 초안 생성 시 프롬프트에 포함되어 일기 품질을 높인다.
     * 비어 있으면 AI는 mood/사진 정보만 보고 작성.
     */
    private String userContent;
}
