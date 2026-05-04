package com.memora.server.repository;

import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByKakaoId(String kakaoId);

    boolean existsByLoginId(String loginId);

    Optional<User> findByResetToken(String resetToken);

    // 리마인드 스케줄러용: FCM 토큰이 있고 오늘 일기를 안 쓴 사용자 조회 (N+1 방지)
    @Query("SELECT u FROM User u WHERE u.fcmToken IS NOT NULL " +
           "AND u.userId NOT IN (SELECT d.user.userId FROM Diary d WHERE d.targetDate = :today)")
    List<User> findUsersWithoutDiaryOnDate(LocalDate today);
}
