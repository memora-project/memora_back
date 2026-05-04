package com.memora.server.repository;

import com.memora.server.entity.Diary;
import com.memora.server.entity.DiarySegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiarySegmentRepository extends JpaRepository<DiarySegment, Integer> {
    // 특정 일기에 속한 세그먼트들을 순서(stepOrder)대로 조회
    List<DiarySegment> findByDiaryOrderByStepOrderAsc(Diary diary);
}