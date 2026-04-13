package com.memora.server.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰의 생성과 검증을 담당하는 클래스
 *
 * 역할:
 * 1. 로그인 성공 시 → Access Token, Refresh Token 생성
 * 2. API 요청 시 → 토큰이 유효한지 검증
 * 3. 토큰에서 사용자 정보(userId) 꺼내기
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    // application.yaml에서 설정값을 주입받는 생성자
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * Access Token 생성
     * - 사용자가 API를 호출할 때 매번 보내는 토큰
     * - 유효 시간: 30분
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenValidity);
    }

    /**
     * Refresh Token 생성
     * - Access Token이 만료됐을 때 새로 발급받기 위한 토큰
     * - 유효 시간: 7일
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenValidity);
    }

    /**
     * 토큰 생성 공통 로직
     *
     * 토큰 안에 들어가는 정보:
     * - subject: userId (누구의 토큰인지)
     * - issuedAt: 발급 시간
     * - expiration: 만료 시간
     * - 서명: secretKey로 암호화 (위조 방지)
     */
    private String createToken(Long userId, long validity) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity);

        return Jwts.builder()
                .subject(userId.toString())  // 토큰의 주인 = userId
                .issuedAt(now)               // 발급 시간
                .expiration(expiry)          // 만료 시간
                .signWith(secretKey)         // 비밀 키로 서명
                .compact();                  // 문자열로 변환
    }

    /**
     * 토큰에서 userId 꺼내기
     * 예: "eyJhbGci..." → 1 (Long)
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰이 유효한지 검증
     * - 서명이 맞는지 (위조 여부)
     * - 만료되지 않았는지
     *
     * 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰 파싱 (해독)
     * secretKey로 서명을 검증하고, 토큰 안의 데이터(Claims)를 꺼냄
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)  // 이 키로 서명 검증
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
