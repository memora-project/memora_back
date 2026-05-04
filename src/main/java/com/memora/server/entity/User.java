package com.memora.server.entity;

import com.memora.server.entity.enums.GenderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(unique = true, length = 50)
    private String loginId;

    @Column(length = 255)
    private String password;

    @Column(unique = true)
    private String kakaoId;

    @Column(length = 15)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    private GenderType gender;

    private LocalDate birthDate;

    private String address;

    @Column(length = 15)
    private String emergencyContact;

    @Column(length = 500)
    private String refreshToken;

    @Column(length = 500)
    private String resetToken;

    @Column(length = 6)
    private String verificationCode;

    private OffsetDateTime verificationCodeExpiry;

    @Column(length = 500)
    private String fcmToken;

    // 사용자 탈퇴 시 관련 데이터 자동 삭제 (cascade + orphanRemoval)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Diary> diaries = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HealthReport> healthReports = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSettings userSettings;

    @Builder.Default
    private Boolean isReportShared = false;

    @Builder.Default
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * Refresh Token 업데이트
     * 로그인 시 저장, 로그아웃 시 null로 삭제
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 회원 정보 수정
     * null이 아닌 필드만 업데이트 (PATCH 방식)
     */
    public void updateProfile(String name, GenderType gender, LocalDate birthDate,
                              String phoneNumber, String address,
                              String emergencyContact, Boolean isReportShared) {
        if (name != null) this.name = name;
        if (gender != null) this.gender = gender;
        if (birthDate != null) this.birthDate = birthDate;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (address != null) this.address = address;
        if (emergencyContact != null) this.emergencyContact = emergencyContact;
        if (isReportShared != null) this.isReportShared = isReportShared;
    }

    /**
     * 비밀번호 재설정 토큰 저장
     */
    public void updateResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    /**
     * 이메일 인증번호 저장 (5분 유효)
     */
    public void updateVerificationCode(String code) {
        this.verificationCode = code;
        this.verificationCodeExpiry = OffsetDateTime.now().plusMinutes(5);
    }

    /**
     * 인증번호 검증
     */
    public boolean isVerificationCodeValid(String code) {
        return this.verificationCode != null
                && this.verificationCode.equals(code)
                && this.verificationCodeExpiry != null
                && OffsetDateTime.now().isBefore(this.verificationCodeExpiry);
    }

    /**
     * 인증번호 초기화
     */
    public void clearVerificationCode() {
        this.verificationCode = null;
        this.verificationCodeExpiry = null;
    }

    /**
     * FCM 토큰 업데이트
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * 비밀번호 변경
     */
    public void updatePassword(String password) {
        this.password = password;
        this.resetToken = null;  // 비밀번호 변경 후 리셋 토큰 삭제
    }
}
