package com.memora.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 모든 API 요청이 Controller에 도착하기 전에 거치는 필터
 *
 * 하는 일:
 * 1. 요청 헤더에서 "Authorization: Bearer xxx" 토큰을 꺼냄
 * 2. JwtTokenProvider로 토큰이 유효한지 검증
 * 3. 유효하면 → Spring Security에 "이 사용자는 인증됨" 이라고 등록
 * 4. 유효하지 않으면 → 아무것도 안 함 (이후 Security 설정에서 차단됨)
 *
 * OncePerRequestFilter: 하나의 요청에 딱 한 번만 실행되는 필터
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1단계: 헤더에서 토큰 꺼내기
        String token = resolveToken(request);

        // 2단계: 토큰이 있고 유효하면 → 인증 정보 등록
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);

            // Spring Security에 "이 사용자는 인증됨" 이라고 알려주는 객체
            // - principal: userId (누구인지)
            // - credentials: null (비밀번호는 이미 로그인 때 확인했으니 필요 없음)
            // - authorities: 빈 리스트 (역할/권한 - 지금은 안 씀)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());

            // SecurityContext에 저장 → 이후 Controller에서 userId를 꺼내 쓸 수 있음
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3단계: 다음 필터 또는 Controller로 넘기기
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 토큰 추출
     *
     * 프론트가 보내는 형식: "Authorization: Bearer eyJhbGci..."
     * → "Bearer " 부분을 잘라내고 순수 토큰만 반환
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);  // "Bearer " = 7글자 제거
        }
        return null;
    }
}
