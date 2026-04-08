package com.memora.server.repository;

import com.memora.server.entity.HealthReport;
import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthReportRepository extends JpaRepository<HealthReport, Long> {
    // 특정 유저의 리포트 목록을 최신순으로 조회
    List<HealthReport> findByUserOrderByEndDateDesc(User user);
}