package com.memora.server.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 로그인 요청 DTO
 *
 * 프론트가 카카오 로그인 후 받은 인가코드를 보내줌:
 * {
 *     "code": "카카오에서 받은 인가코드",
 *     "redirectUri": "http://localhost:5173/oauth/kakao"
 * }
 */
@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    private String code;
    private String redirectUri;
}
