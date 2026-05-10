package com.memora.server.repository;

import com.memora.server.entity.Diary;
import com.memora.server.entity.DiarySegment;
import com.memora.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiarySegmentRepository extends JpaRepository<DiarySegment, Integer> {
    // 특정 일기에 속한 세그먼트들을 순서(stepOrder)대로 조회
    List<DiarySegment> findByDiaryOrderByStepOrderAsc(Diary diary);

    // 퀴즈용: 사진+위치 정보가 있는 세그먼트 조회
    @Query("SELECT s FROM DiarySegment s WHERE s.diary.user = :user " +
           "AND s.photoUrl IS NOT NULL AND s.locationName IS NOT NULL " +
           "AND s.locationName <> ''")
    List<DiarySegment> findQuizCandidates(User user);
}