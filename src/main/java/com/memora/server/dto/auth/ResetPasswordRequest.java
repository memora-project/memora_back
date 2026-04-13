package com.memora.server.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 요청 DTO
 *
 * 본인 확인: loginId + phoneNumber 일치 여부로 검증
 * { "loginId": "memora123", "phoneNumber": "010-1234-5678" }
 */
@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    private String loginId;
    private String phoneNumber;
}
