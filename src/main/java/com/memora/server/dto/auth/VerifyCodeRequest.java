package com.memora.server.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증번호 확인 요청 DTO
 *
 * { "loginId": "memora@gmail.com", "code": "123456" }
 */
@Getter
@NoArgsConstructor
public class VerifyCodeRequest {

    private String loginId;
    private String code;
}
