package com.memora.server.controller;

import com.memora.server.dto.settings.SettingsResponse;
import com.memora.server.dto.settings.SettingsUpdateRequest;
import com.memora.server.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 설정 관련 API 엔드포인트
 *
 * 글씨 크기, 알림 설정 등
 * 토큰 필수
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * 내 설정 조회
     * GET /api/v1/settings
     *
     * 응답: { fontSize: "LARGE", notificationEnabled: true, reminderTime: "20:00" }
     */
    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings() {
        Integer userId = getCurrentUserId();
        SettingsResponse response = settingsService.getSettings(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 설정 변경
     * PATCH /api/v1/settings
     *
     * 요청: { fontSize: "MEDIUM" }
     */
    @PatchMapping
    public ResponseEntity<SettingsResponse> updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        Integer userId = getCurrentUserId();
        SettingsResponse response = settingsService.updateSettings(userId, request);
        return ResponseEntity.ok(response);
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
