package com.memora.server.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 카카오 API와 통신하는 서비스
 *
 * 웹 로그인 흐름:
 * 1. 프론트가 카카오 로그인 페이지로 보냄
 * 2. 사용자가 로그인하면 카카오가 "인가코드(code)"를 줌
 * 3. 백엔드가 인가코드로 카카오 Access Token을 받음
 * 4. 카카오 Access Token으로 사용자 정보를 가져옴
 */
@Service
public class KakaoService {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    /**
     * 인가코드 → 카카오 Access Token 교환
     *
     * 카카오에 POST 요청:
     * grant_type=authorization_code
     * client_id=REST API 키
     * redirect_uri=프론트 리다이렉트 주소
     * code=인가코드
     *
     * 카카오 응답: { "access_token": "xxx", ... }
     */
    public String getAccessToken(String code, String redirectUri) {
        String body = "grant_type=authorization_code"
                + "&client_id=" + restApiKey
                + "&client_secret=" + clientSecret
                + "&redirect_uri=" + redirectUri
                + "&code=" + code;

        Map response = restClient.post()
                .uri(KAKAO_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalArgumentException("카카오 토큰을 가져올 수 없습니다.");
        }

        return (String) response.get("access_token");
    }

    /**
     * 카카오 Access Token으로 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        Map response = restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalArgumentException("카카오 사용자 정보를 가져올 수 없습니다.");
        }

        String kakaoId = String.valueOf(response.get("id"));

        Map properties = (Map) response.get("properties");
        String nickname = properties != null ? (String) properties.get("nickname") : "카카오 사용자";

        return new KakaoUserInfo(kakaoId, nickname);
    }

    @Getter
    public static class KakaoUserInfo {
        private final String kakaoId;
        private final String nickname;

        public KakaoUserInfo(String kakaoId, String nickname) {
            this.kakaoId = kakaoId;
            this.nickname = nickname;
        }
    }
}
