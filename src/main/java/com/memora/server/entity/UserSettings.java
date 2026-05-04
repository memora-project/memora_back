package com.memora.server.entity;

import com.memora.server.entity.enums.FontSize;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * 사용자 설정 엔티티
 *
 * User와 1:1 관계
 * 글씨 크기, 알림 설정 등 사용자 환경 설정을 저장
 */
@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FontSize fontSize = FontSize.LARGE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean notificationEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private LocalTime reminderTime = LocalTime.of(20, 0);

    /**
     * 설정 변경
     */
    public void update(FontSize fontSize, Boolean notificationEnabled, LocalTime reminderTime) {
        if (fontSize != null) this.fontSize = fontSize;
        if (notificationEnabled != null) this.notificationEnabled = notificationEnabled;
        if (reminderTime != null) this.reminderTime = reminderTime;
    }
}
