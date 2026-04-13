package com.memora.server.dto.segment;

import com.memora.server.entity.enums.MoodType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 중간 기록 수정 요청 DTO
 *
 * AI 초안을 확인 후 사용자가 수정할 때 사용
 * 기분도 다시 선택 가능
 *
 * 예시: { "moodSnapshot": "CALM", "userContent": "수정된 내용" }
 */
@Getter
@NoArgsConstructor
public class SegmentUpdateRequest {

    private MoodType moodSnapshot;
    private String userContent;
}
