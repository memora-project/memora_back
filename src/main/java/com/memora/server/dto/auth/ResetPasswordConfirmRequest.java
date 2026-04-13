package com.memora.server.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 완료 DTO
 *
 * 리셋 토큰 + 새 비밀번호
 * { "resetToken": "abc123...", "newPassword": "newpass" }
 */
@Getter
@NoArgsConstructor
public class ResetPasswordConfirmRequest {

    private String resetToken;
    private String newPassword;
}
