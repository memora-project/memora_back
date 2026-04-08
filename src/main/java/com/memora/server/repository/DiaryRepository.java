package com.memora.server.repository;

import com.memora.server.entity.Diary;
import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    // 특정 유저의 특정 날짜 일기가 있는지 조회
    Optional<Diary> findByUserAndTargetDate(User user, LocalDate targetDate);
}
