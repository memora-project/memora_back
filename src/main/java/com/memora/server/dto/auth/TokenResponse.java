package com.memora.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 응답 DTO
 *
 * 로그인 성공 시 프론트에 보내는 데이터:
 * {
 *     "accessToken": "eyJhbGci...",
 *     "refreshToken": "eyJhbGci..."
 * }
 *
 * 프론트는 이 토큰을 저장해두고,
 * API 호출할 때마다 accessToken을 헤더에 담아서 보냄
 */
@Getter
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
}
