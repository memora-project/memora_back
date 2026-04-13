package com.memora.server.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 재발급 요청 DTO
 *
 * Access Token이 만료됐을 때 프론트가 보내는 데이터:
 * {
 *     "refreshToken": "eyJhbGci..."
 * }
 */
@Getter
@NoArgsConstructor
public class RefreshRequest {

    private String refreshToken;
}
