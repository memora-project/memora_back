package com.memora.server.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 *
 * 프론트가 로그인할 때 보내는 데이터:
 * {
 *     "loginId": "memora123",
 *     "password": "1234"
 * }
 */
@Getter
@NoArgsConstructor
public class LoginRequest {

    private String loginId;
    private String password;
}
