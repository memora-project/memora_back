package com.memora.server.dto.settings;

import com.memora.server.entity.UserSettings;
import com.memora.server.entity.enums.FontSize;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

/**
 * 설정 응답 DTO
 *
 * {
 *     "fontSize": "LARGE",
 *     "notificationEnabled": true,
 *     "reminderTime": "20:00"
 * }
 */
@Getter
@AllArgsConstructor
public class SettingsResponse {

    private FontSize fontSize;
    private Boolean notificationEnabled;
    private LocalTime reminderTime;

    public static SettingsResponse from(UserSettings settings) {
        return new SettingsResponse(
                settings.getFontSize(),
                settings.getNotificationEnabled(),
                settings.getReminderTime()
        );
    }
}
