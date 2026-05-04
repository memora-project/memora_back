package com.memora.server.service;

import com.memora.server.dto.diary.DiaryResponse;
import com.memora.server.dto.diary.DiaryUpdateRequest;
import com.memora.server.entity.Diary;
import com.memora.server.entity.User;
import com.memora.server.repository.DiaryRepository;
import com.memora.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 일기 관련 비즈니스 로직
 *
 * 일기 생성 → 중간 기록 추가 → 최종 수정 → 완료 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    /**
     * 일기 작성 시작
     *
     * 오늘 날짜로 빈 일기를 생성
     * 이미 오늘 일기가 있으면 → 기존 일기를 반환 (중복 생성 방지)
     */
    @Transactional
    public DiaryResponse createDiary(Integer userId) {
        User user = findUserById(userId);
        LocalDate today = LocalDate.now();

        // 오늘 일기가 이미 있으면 그걸 반환
        return diaryRepository.findByUserAndTargetDate(user, today)
                .map(DiaryResponse::from)
                .orElseGet(() -> {
                    Diary diary = Diary.builder()
                            .user(user)
                            .targetDate(today)
                            .build();
                    return DiaryResponse.from(diaryRepository.save(diary));
                });
    }

    /**
     * 월별 일기 목록 조회
     *
     * month 파라미터 예: "2026-04"
     * → 2026년 4월 1일 ~ 4월 30일 사이의 일기를 최신순으로 조회
     *
     * 캘린더 모드에서 한 달치 일기를 보여줄 때 사용
     */
    public List<DiaryResponse> getDiariesByMonth(Integer userId, String month) {
        User user = findUserById(userId);

        // "2026-04" → YearMonth → 시작일/종료일 계산
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);           // 4월 1일
        LocalDate endDate = yearMonth.atEndOfMonth();       // 4월 30일

        return diaryRepository
                .findByUserAndTargetDateBetweenOrderByTargetDateDesc(user, startDate, endDate)
                .stream()
                .map(DiaryResponse::from)
                .toList();
    }

    /**
     * 특정 일기 상세 조회
     *
     * diaryId로 일기를 찾고, 본인 일기인지 확인
     */
    public DiaryResponse getDiary(Integer userId, Integer diaryId) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);
        return DiaryResponse.from(diary);
    }

    /**
     * 최종 일기 수정
     *
     * 최종 기분(finalMood)과 내용(finalContent)을 수정
     * AI 초안을 확인하고 사용자가 수정할 때 사용
     */
    @Transactional
    public DiaryResponse updateDiary(Integer userId, Integer diaryId, DiaryUpdateRequest request) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);
        diary.updateFinal(request.getFinalMood(), request.getFinalContent());
        return DiaryResponse.from(diary);
    }

    /**
     * 최종 일기 완료 처리
     *
     * status: IN_PROGRESS → COMPLETED
     * 최종 기분이 선택되어 있어야 완료 가능
     */
    @Transactional
    public DiaryResponse completeDiary(Integer userId, Integer diaryId) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);

        if (diary.getFinalMood() == null) {
            throw new IllegalArgumentException("최종 기분을 선택해야 완료할 수 있습니다.");
        }

        diary.complete();
        return DiaryResponse.from(diary);
    }

    /**
     * 일기 삭제
     *
     * Diary 엔티티에 cascade = ALL, orphanRemoval = true 설정으로
     * 일기 삭제 시 중간기록(segments)도 자동 삭제됨
     */
    @Transactional
    public void deleteDiary(Integer userId, Integer diaryId) {
        Diary diary = findDiaryByIdAndUser(diaryId, userId);
        diaryRepository.delete(diary);
    }

    /**
     * diaryId + userId로 일기 찾기 (본인 일기인지 확인)
     *
     * 다른 사람의 일기에 접근하려 하면 예외 발생
     * → 보안상 중요한 체크!
     */
    private Diary findDiaryByIdAndUser(Integer diaryId, Integer userId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        if (!diary.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return diary;
    }

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
