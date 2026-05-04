package com.memora.server.repository;

import com.memora.server.entity.HealthReport;
import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthReportRepository extends JpaRepository<HealthReport, Integer> {
    // 특정 유저의 리포트 목록을 최신순으로 조회
    List<HealthReport> findByUserOrderByEndDateDesc(User user);

    // 특정 유저의 특정 기간 리포트 조회 (전체 로드 방지)
    Optional<HealthReport> findByUserAndStartDateAndEndDate(User user, LocalDate startDate, LocalDate endDate);
}