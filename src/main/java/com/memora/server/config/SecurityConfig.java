package com.memora.server.config;

import com.memora.server.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 *
 * 역할:
 * 1. URL별 접근 권한 설정 (로그인 필요 여부)
 * 2. JWT 필터 등록 (매 요청마다 토큰 확인)
 * 3. 비밀번호 암호화 방식 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 보안 필터 체인 설정
     *
     * 요청이 들어오면 이 규칙을 순서대로 확인:
     * 1. CSRF 비활성화 (REST API는 CSRF 방어가 필요 없음 - 토큰 방식이니까)
     * 2. 세션 사용 안 함 (JWT 방식이니까 서버에 세션 저장 X)
     * 3. URL별 접근 규칙 설정
     * 4. JWT 필터를 Spring Security 기본 필터 앞에 끼워 넣기
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 허용: 프론트(localhost:5173)에서 백엔드(localhost:8080)로 요청 허용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화: REST API는 브라우저 폼이 아닌 토큰으로 인증하므로 불필요
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안 함: JWT는 토큰 자체에 인증 정보가 있으므로 서버 세션 불필요
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 URL (No Login)
                        .requestMatchers(
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/kakao",
                                "/api/v1/auth/check-id",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/reset-password/confirm",
                                "/uploads/**"
                        ).permitAll()

                        // 그 외 모든 요청은 인증 필요 (User)
                        .anyRequest().authenticated()
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                // → Spring Security의 기본 로그인 처리보다 먼저 JWT 토큰을 확인
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 암호화 도구
     *
     * BCrypt: 업계 표준 암호화 방식
     * - "1234" → "$2a$10$N9qo8uLOickgx2ZMRZoMye..." (복호화 불가능)
     * - 같은 비밀번호도 매번 다른 결과 (salt 사용)
     * - 회원가입 시 암호화해서 저장, 로그인 시 비교
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정
     * 프론트엔드(localhost:5173)에서 백엔드로 요청할 수 있게 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://192.168.*:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
