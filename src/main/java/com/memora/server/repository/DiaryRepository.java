package com.memora.server.repository;

import com.memora.server.entity.Diary;
import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.memora.server.entity.enums.DiaryStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Integer> {

    Optional<Diary> findByUserAndTargetDate(User user, LocalDate targetDate);

    List<Diary> findByUserAndTargetDateBetweenOrderByTargetDateDesc(
            User user, LocalDate startDate, LocalDate endDate);

    List<Diary> findByUserOrderByTargetDateDesc(User user);

    // 리포트용: 특정 기간의 완료된 일기만 조회
    List<Diary> findByUserAndStatusAndTargetDateBetween(
            User user, DiaryStatus status, LocalDate startDate, LocalDate endDate);

    // 자정 스케줄러용: 특정 날짜의 IN_PROGRESS 일기 조회
    List<Diary> findByStatusAndTargetDate(DiaryStatus status, LocalDate targetDate);
}
