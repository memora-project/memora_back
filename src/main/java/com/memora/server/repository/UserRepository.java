package com.memora.server.repository;

import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByKakaoId(String kakaoId);

    boolean existsByLoginId(String loginId);

    Optional<User> findByResetToken(String resetToken);
}
