package com.memora.server.controller;

import jakarta.validation.Valid;
import com.memora.server.dto.auth.KakaoLoginRequest;
import com.memora.server.dto.auth.LoginRequest;
import com.memora.server.dto.auth.RefreshRequest;
import com.memora.server.dto.auth.ResetPasswordConfirmRequest;
import com.memora.server.dto.auth.ResetPasswordRequest;
import com.memora.server.dto.auth.SignupRequest;
import com.memora.server.dto.auth.TokenResponse;
import com.memora.server.dto.auth.VerifyCodeRequest;
import com.memora.server.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 API 엔드포인트
 *
 * 모든 URL은 /api/v1/auth 로 시작
 * SecurityConfig에서 이 URL들은 permitAll() → 토큰 없이 접근 가능
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     * POST /api/v1/auth/signup
     *
     * 요청: { loginId, password, name, gender, birthDate, ... }
     * 응답: { accessToken, refreshToken }
     *
     * @RequestBody: 프론트가 보낸 JSON을 SignupRequest 객체로 자동 변환
     * HttpStatus.CREATED (201): "새로운 리소스(회원)가 생성됐다"는 의미
     */
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        TokenResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인
     * POST /api/v1/auth/login
     *
     * 요청: { loginId, password }
     * 응답: { accessToken, refreshToken }
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 로그인
     * POST /api/v1/auth/kakao
     *
     * 토큰 없이 접근 가능
     * 요청: { code: "카카오 인가코드", redirectUri: "http://localhost:5173/oauth/kakao" }
     * 응답: { accessToken: "우리 JWT", refreshToken: "우리 JWT" }
     *
     * 처음 로그인하면 자동 회원가입 후 토큰 발급
     * 이미 가입된 사용자면 바로 토큰 발급
     */
    @PostMapping("/kakao")
    public ResponseEntity<TokenResponse> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        TokenResponse response = authService.kakaoLogin(request.getCode(), request.getRedirectUri());
        return ResponseEntity.ok(response);
    }

    /**
     * 아이디 중복 체크
     * GET /api/v1/auth/check-id?loginId=memora123
     *
     * 응답: { "exists": true }  → 이미 사용 중 (사용 불가)
     * 응답: { "exists": false } → 사용 가능
     *
     * @RequestParam: URL의 ?loginId=xxx 부분을 변수로 받음
     * Map.of(): 간단한 JSON 응답을 만드는 방법
     */
    @GetMapping("/check-id")
    public ResponseEntity<Map<String, Boolean>> checkId(@RequestParam String loginId) {
        boolean exists = authService.checkLoginIdExists(loginId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     *
     * 토큰 필요 (로그인한 사용자만 가능)
     * DB에서 Refresh Token 삭제 → 재발급 불가 → 로그아웃
     *
     * SecurityContextHolder: JwtAuthenticationFilter에서 저장한 인증 정보를 꺼냄
     * → principal에 userId가 들어있음
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Integer userId = (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    /**
     * Access Token 재발급
     * POST /api/v1/auth/refresh
     *
     * 토큰 없이 접근 가능 (Access Token이 만료된 상태에서 호출하니까)
     * 요청: { refreshToken: "eyJhbGci..." }
     * 응답: { accessToken: "새 토큰", refreshToken: "기존 토큰" }
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 재설정 요청 - 이메일로 인증번호 발송
     * POST /api/v1/auth/reset-password
     *
     * 요청: { loginId: "memora@gmail.com" }
     * 응답: { message: "인증번호가 이메일로 발송되었습니다." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.requestPasswordReset(request.getLoginId());
        return ResponseEntity.ok(Map.of("message", "인증번호가 이메일로 발송되었습니다."));
    }

    /**
     * 인증번호 확인
     * POST /api/v1/auth/verify-code
     *
     * 요청: { loginId: "memora@gmail.com", code: "123456" }
     * 응답: { resetToken: "abc123..." }
     */
    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, String>> verifyCode(@RequestBody VerifyCodeRequest request) {
        String resetToken = authService.verifyCode(request);
        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }

    /**
     * 비밀번호 재설정 완료
     * POST /api/v1/auth/reset-password/confirm
     *
     * 요청: { resetToken, newPassword }
     * 응답: { message: "비밀번호가 변경되었습니다." }
     */
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Map<String, String>> resetPasswordConfirm(@RequestBody ResetPasswordConfirmRequest request) {
        authService.confirmPasswordReset(request.getResetToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }
}
