package com.memora.server.dto.settings;

import com.memora.server.entity.enums.FontSize;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 설정 변경 요청 DTO
 *
 * 바꾸고 싶은 항목만 보내면 됨
 * 예: { "fontSize": "MEDIUM" }
 * 예: { "notificationEnabled": false, "reminderTime": "21:00" }
 */
@Getter
@NoArgsConstructor
public class SettingsUpdateRequest {

    private FontSize fontSize;
    private Boolean notificationEnabled;
    private LocalTime reminderTime;
}
