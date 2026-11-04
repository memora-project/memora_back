package com.memora.server.service;

import com.memora.server.entity.User;
import com.memora.server.repository.UserRepository;
import com.memora.server.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BE-P1-12: resetToken 없이 재설정 확정을 보내면 조회 전에 거부되는지 확인.
 */
class AuthServiceResetPasswordTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = new AuthService(userRepository, passwordEncoder,
                mock(JwtTokenProvider.class), mock(KakaoService.class), mock(EmailService.class));
    }

    @Test
    void null_토큰은_조회없이_거부된다() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> authService.confirmPasswordReset(null, "newpass"));
        assertEquals("유효하지 않은 토큰입니다.", e.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void 빈_토큰과_공백_토큰은_조회없이_거부된다() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.confirmPasswordReset("", "newpass"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.confirmPasswordReset("   ", "newpass"));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void 정상_토큰이면_비밀번호가_변경된다() {
        User user = mock(User.class);
        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded");

        authService.confirmPasswordReset("valid-token", "newpass");

        verify(user).updatePassword("encoded");
    }

    @Test
    void 없는_토큰은_기존처럼_거부된다() {
        when(userRepository.findByResetToken(any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> authService.confirmPasswordReset("unknown", "newpass"));
    }
}
