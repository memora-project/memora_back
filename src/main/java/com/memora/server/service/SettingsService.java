package com.memora.server.service;

import com.memora.server.dto.settings.SettingsResponse;
import com.memora.server.dto.settings.SettingsUpdateRequest;
import com.memora.server.entity.User;
import com.memora.server.entity.UserSettings;
import com.memora.server.repository.UserRepository;
import com.memora.server.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설정 관련 비즈니스 로직
 *
 * 설정이 없으면 기본값으로 자동 생성
 * (글씨: LARGE, 알림: ON, 리마인드: 20:00)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    /**
     * 내 설정 조회
     * 설정이 없으면 기본값으로 생성
     */
    @Transactional
    public SettingsResponse getSettings(Integer userId) {
        UserSettings settings = getOrCreateSettings(userId);
        return SettingsResponse.from(settings);
    }

    /**
     * 설정 변경
     */
    @Transactional
    public SettingsResponse updateSettings(Integer userId, SettingsUpdateRequest request) {
        UserSettings settings = getOrCreateSettings(userId);
        settings.update(request.getFontSize(), request.getNotificationEnabled(), request.getReminderTime(), request.getAutoCompleteTime());
        return SettingsResponse.from(settings);
    }

    /**
     * 설정 가져오기 (없으면 기본값으로 생성)
     *
     * 회원가입 시 설정을 안 만들어도
     * 처음 설정 조회할 때 자동 생성됨
     */
    private UserSettings getOrCreateSettings(Integer userId) {
        return settingsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    UserSettings newSettings = UserSettings.builder()
                            .user(user)
                            .build();
                    return settingsRepository.save(newSettings);
                });
    }
}
