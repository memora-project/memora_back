package com.memora.server.service;

import com.memora.server.dto.auth.LoginRequest;
import com.memora.server.dto.auth.SignupRequest;
import com.memora.server.dto.auth.TokenResponse;

import java.util.UUID;
import com.memora.server.entity.User;
import com.memora.server.repository.UserRepository;
import com.memora.server.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직
 *
 * 회원가입: 비밀번호 암호화 → DB 저장
 * 로그인: 비밀번호 확인 → JWT 토큰 발급
 * 아이디 중복 체크: DB에 이미 있는지 확인
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 (성능 최적화)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoService kakaoService;

    /**
     * 회원가입
     *
     * 흐름:
     * 1. 아이디 중복 체크
     * 2. 비밀번호 암호화 ("1234" → "$2a$10$N9qo8u...")
     * 3. User 엔티티 생성 후 DB에 저장
     * 4. 바로 토큰 발급 (가입 후 자동 로그인)
     */
    @Transactional  // 쓰기 작업이므로 readOnly 해제
    public TokenResponse signup(SignupRequest request) {
        // 1. 아이디 중복 체크
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 2. User 엔티티 생성 (Builder 패턴 사용)
        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))  // 비밀번호 암호화!
                .name(request.getName())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .emergencyContact(request.getEmergencyContact())
                .build();

        // 3. DB에 저장
        User savedUser = userRepository.save(user);

        // 4. 토큰 발급 + Refresh Token DB에 저장 (가입 후 자동 로그인)
        return createAndSaveTokens(savedUser);
    }

    /**
     * 로그인
     *
     * 흐름:
     * 1. loginId로 사용자 조회
     * 2. 비밀번호 일치 확인 (암호화된 값끼리 비교)
     * 3. 토큰 발급
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. 사용자 조회 (없으면 예외)
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 2. 비밀번호 확인
        //    passwordEncoder.matches("입력한 비번", "DB에 저장된 암호화 비번")
        //    → 내부적으로 암호화해서 비교해줌
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 3. 토큰 발급 + Refresh Token DB에 저장
        return createAndSaveTokens(user);
    }

    /**
     * 카카오 로그인
     *
     * 흐름:
     * 1. 인가코드로 카카오 Access Token 받기
     * 2. 카카오 Access Token으로 사용자 정보 가져오기
     * 3. kakaoId로 DB 조회
     *    → 이미 있으면: 기존 사용자로 로그인
     *    → 없으면: 자동 회원가입 후 로그인
     * 4. JWT 토큰 발급
     */
    @Transactional
    public TokenResponse kakaoLogin(String code, String redirectUri) {
        // 1. 인가코드 → 카카오 Access Token
        String kakaoAccessToken = kakaoService.getAccessToken(code, redirectUri);

        // 2. 카카오 Access Token → 사용자 정보
        KakaoService.KakaoUserInfo kakaoUserInfo = kakaoService.getUserInfo(kakaoAccessToken);

        // 2. kakaoId로 기존 사용자인지 확인
        User user = userRepository.findByKakaoId(kakaoUserInfo.getKakaoId())
                .orElseGet(() -> {
                    // 없으면 → 자동 회원가입
                    User newUser = User.builder()
                            .kakaoId(kakaoUserInfo.getKakaoId())
                            .name(kakaoUserInfo.getNickname())
                            .build();
                    return userRepository.save(newUser);
                });

        // 3. JWT 토큰 발급
        return createAndSaveTokens(user);
    }

    /**
     * 아이디 중복 체크
     *
     * true = 이미 존재함 (사용 불가)
     * false = 사용 가능
     */
    public boolean checkLoginIdExists(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    /**
     * 비밀번호 재설정 요청
     *
     * 1. loginId + phoneNumber로 본인 확인
     * 2. UUID 리셋 토큰 생성 후 DB에 저장
     * 3. 리셋 토큰 반환 (실제로는 이메일/문자로 보내야 함)
     */
    @Transactional
    public String requestPasswordReset(String loginId, String phoneNumber) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디를 찾을 수 없습니다."));

        if (user.getPhoneNumber() == null || !user.getPhoneNumber().equals(phoneNumber)) {
            throw new IllegalArgumentException("전화번호가 일치하지 않습니다.");
        }

        String resetToken = UUID.randomUUID().toString();
        user.updateResetToken(resetToken);
        return resetToken;
    }

    /**
     * 비밀번호 재설정 완료
     *
     * 1. 리셋 토큰으로 사용자 찾기
     * 2. 새 비밀번호 암호화 후 저장
     * 3. 리셋 토큰 삭제
     */
    @Transactional
    public void confirmPasswordReset(String resetToken, String newPassword) {
        User user = userRepository.findByResetToken(resetToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    /**
     * 로그아웃
     *
     * DB에서 Refresh Token을 삭제 → 재발급 불가 → 사실상 로그아웃
     * Access Token은 만료될 때까지 유효하지만, 보통 30분이라 큰 문제 없음
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.updateRefreshToken(null);  // Refresh Token 삭제
    }

    /**
     * Access Token 재발급
     *
     * 흐름:
     * 1. Refresh Token 유효성 검증
     * 2. 토큰에서 userId 추출
     * 3. DB에 저장된 Refresh Token과 일치하는지 확인
     * 4. 새 Access Token 발급 (Refresh Token은 그대로 유지)
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        // 1. Refresh Token 유효한지 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 토큰입니다. 다시 로그인해주세요.");
        }

        // 2. 토큰에서 userId 추출
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 3. DB에 저장된 Refresh Token과 비교
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다. 다시 로그인해주세요.");
        }

        // 4. 새 Access Token만 발급 (Refresh Token은 유지)
        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        return new TokenResponse(newAccessToken, refreshToken);
    }

    /**
     * 토큰 생성 + Refresh Token DB 저장
     */
    private TokenResponse createAndSaveTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // Refresh Token을 DB에 저장 (로그아웃 시 삭제용)
        user.updateRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }
}
