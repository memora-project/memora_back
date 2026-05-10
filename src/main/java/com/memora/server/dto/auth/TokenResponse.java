package com.memora.server.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 응답 DTO
 *
 * 로그인 성공 시 프론트에 보내는 데이터:
 * {
 *     "accessToken": "eyJhbGci...",
 *     "refreshToken": "eyJhbGci...",
 *     "isNewUser": false
 * }
 *
 * isNewUser가 true면 프론트에서 추가 정보 입력 화면으로 이동
 */
@Getter
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private Boolean isNewUser;

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.isNewUser = false;
    }
}
