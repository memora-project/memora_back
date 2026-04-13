package com.memora.server.service;

import com.memora.server.dto.user.UserResponse;
import com.memora.server.dto.user.UserUpdateRequest;
import com.memora.server.entity.User;
import com.memora.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 관련 비즈니스 로직
 *
 * 내 정보 조회, 수정, 탈퇴
 * 모든 메서드에 userId가 들어옴 (토큰에서 추출한 값)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 내 정보 조회
     */
    public UserResponse getMyInfo(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    /**
     * 내 정보 수정
     *
     * JPA의 더티 체킹(Dirty Checking) 활용:
     * 엔티티 값을 바꾸면 트랜잭션 끝날 때 자동으로 UPDATE 쿼리 실행
     * → repository.save() 안 해도 됨!
     */
    @Transactional
    public UserResponse updateMyInfo(Long userId, UserUpdateRequest request) {
        User user = findUserById(userId);

        user.updateProfile(
                request.getName(),
                request.getGender(),
                request.getBirthDate(),
                request.getPhoneNumber(),
                request.getAddress(),
                request.getEmergencyContact(),
                request.getIsReportShared()
        );

        return UserResponse.from(user);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        userRepository.delete(user);
    }

    /**
     * userId로 사용자 찾기 (공통)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
