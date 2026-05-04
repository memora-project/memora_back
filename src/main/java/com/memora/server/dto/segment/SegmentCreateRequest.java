package com.memora.server.dto.segment;

import com.memora.server.entity.enums.MoodType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 중간 기록 생성 요청 DTO
 *
 * 기분 선택 + 사진 정보(선택) + 한 줄 메모(선택)를 보내면 중간 기록이 생성됨
 *
 * 예시:
 * {
 *     "moodSnapshot": "GREAT",
 *     "photoUrl": "https://...",
 *     "takenAt": "2026-04-12T10:30:00+09:00",
 *     "latitude": 36.3504,
 *     "longitude": 127.3845,
 *     "locationName": "대전 유성구",
 *     "userContent": "오랜만에 동네 산책"
 * }
 *
 * 사진 없이 기분만 보내도 됨:
 * { "moodSnapshot": "CALM" }
 */
@Getter
@NoArgsConstructor
public class SegmentCreateRequest {

    private MoodType moodSnapshot;
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
