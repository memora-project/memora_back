package com.memora.server.dto.diary;

import com.memora.server.entity.Diary;
import com.memora.server.entity.enums.DiaryStatus;
import com.memora.server.entity.enums.MoodType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 일기 응답 DTO
 *
 * 응답 예시:
 * {
 *     "diaryId": 1,
 *     "targetDate": "2026-04-12",
 *     "finalMood": "GREAT",
 *     "aiDraft": "AI가 작성한 초안...",
 *     "finalContent": "사용자가 수정한 내용...",
 *     "isEdited": false,
 *     "status": "IN_PROGRESS",
 *     "createdAt": "...",
 *     "updatedAt": "..."
 * }
 */
@Getter
@AllArgsConstructor
public class DiaryResponse {

    private Long diaryId;
    private LocalDate targetDate;
    private MoodType finalMood;
    private String aiDraft;
    private String finalContent;
    private Boolean isEdited;
    private DiaryStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.getDiaryId(),
                diary.getTargetDate(),
                diary.getFinalMood(),
                diary.getAiDraft(),
                diary.getFinalContent(),
                diary.getIsEdited(),
                diary.getStatus(),
                diary.getCreatedAt(),
                diary.getUpdatedAt()
        );
    }
}
