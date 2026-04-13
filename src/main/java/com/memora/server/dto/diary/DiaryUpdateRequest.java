package com.memora.server.dto.diary;

import com.memora.server.entity.enums.MoodType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 최종 일기 수정 요청 DTO
 *
 * 최종 일기를 완료하기 전에 수정할 때 사용
 * 예: { "finalMood": "GREAT", "finalContent": "오늘 하루 정말 좋았다." }
 */
@Getter
@NoArgsConstructor
public class DiaryUpdateRequest {

    private MoodType finalMood;
    private String finalContent;
}
