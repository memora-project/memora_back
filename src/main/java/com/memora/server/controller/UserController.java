package com.memora.server.controller;

import com.memora.server.dto.user.UserResponse;
import com.memora.server.dto.user.UserUpdateRequest;
import com.memora.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 회원 관련 API 엔드포인트
 *
 * 모든 URL은 /api/v1/users 로 시작
 * 전부 토큰 필수 (로그인한 사용자만 접근 가능)
 *
 * "/me" = 토큰에서 꺼낸 userId로 "내" 정보를 처리
 * → 다른 사람 정보는 절대 접근 못함!
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/v1/users/me
     *
     * 응답: { userId, loginId, name, gender, birthDate, ... }
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo() {
        Long userId = getCurrentUserId();
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 정보 수정
     * PATCH /api/v1/users/me
     *
     * 요청: { name: "새이름" }  ← 수정할 필드만 보내면 됨
     * 응답: 수정된 전체 회원 정보
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(@RequestBody UserUpdateRequest request) {
        Long userId = getCurrentUserId();
        UserResponse response = userService.updateMyInfo(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 탈퇴
     * DELETE /api/v1/users/me
     *
     * 응답: { message: "탈퇴 처리되었습니다." }
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteUser() {
        Long userId = getCurrentUserId();
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "탈퇴 처리되었습니다."));
    }

    /**
     * 현재 로그인한 사용자의 userId 꺼내기
     *
     * JwtAuthenticationFilter에서 SecurityContext에 저장한 인증 정보에서
     * principal(= userId)을 꺼냄
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
