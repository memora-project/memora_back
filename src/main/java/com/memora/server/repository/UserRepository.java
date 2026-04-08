package com.memora.server.repository;

import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 기본적으로 save(), findById(), findAll(), delete() 등은 이미 들어있습니다.

    // 추가로 필요한 기능들 (메소드 이름만으로 쿼리가 자동 생성됨)

    // 1. 로그인 아이디로 사용자 찾기
    Optional<User> findByLoginId(String loginId);

    // 3. 이름으로 사용자 리스트 찾기
    // List<User> findByName(String name);
}