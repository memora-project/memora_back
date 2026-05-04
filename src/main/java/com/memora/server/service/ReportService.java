package com.memora.server.service;

import com.memora.server.dto.report.ReportResponse;
import com.memora.server.entity.Diary;
import com.memora.server.entity.HealthReport;
import com.memora.server.entity.User;
import com.memora.server.entity.enums.MoodType;
import com.memora.server.repository.DiaryRepository;
import com.memora.server.repository.HealthReportRepository;
import com.memora.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 리포트 관련 비즈니스 로직
 *
 * 주간/월간 기분 통계를 집계하고 리포트를 생성
 * 기분 분포, 가장 빈번한 기분, 활동 점수 등을 계산
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final HealthReportRepository reportRepository;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    /**
     * 주간 리포트 조회
     *
     * 이번 주 월요일 ~ 일요일 기준으로 집계
     * 리포트가 없으면 새로 생성
     */
    @Transactional
    public ReportResponse getWeeklyReport(Integer userId) {
        User user = findUserById(userId);

        // 이번 주 월~일 계산
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return generateReport(user, startDate, endDate);
    }

    /**
     * 월간 리포트 조회
     *
     * 이번 달 1일 ~ 말일 기준으로 집계
     * 리포트가 없으면 새로 생성
     */
    @Transactional
    public ReportResponse getMonthlyReport(Integer userId) {
        User user = findUserById(userId);

        YearMonth yearMonth = YearMonth.now();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return generateReport(user, startDate, endDate);
    }

    /**
     * 리포트 공유 처리
     *
     * isShared = true로 변경
     * (실제 전송 로직은 추후 구현 - 카카오 메시지, 문자 등)
     */
    @Transactional
    public ReportResponse shareReport(Integer userId, Integer reportId) {
        HealthReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        if (!report.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        report.share();

        // 리포트 기간에 해당하는 기분 분포 재계산
        User user = report.getUser();
        Aggregation agg = aggregateMoods(user, report.getStartDate(), report.getEndDate());
        return ReportResponse.from(report, agg.distribution, agg.totalEntries);
    }

    /**
     * 리포트 생성/갱신 공통 로직
     *
     * 사용자가 일반 일기(segment)를 추가할 때마다 분석에 즉시 반영되도록
     * 모든 status의 diary를 대상으로 segments + finalMood까지 모두 카운트한다.
     *
     * 1. 해당 기간의 모든 일기 조회 (status 무관)
     * 2. 모든 segment.moodSnapshot + (있으면) finalMood 합산
     * 3. 가장 빈번한 기분 결정
     * 4. 총 작성 수 = 모든 segments + 마무리 일기(있는 경우)
     * 5. DB에 저장 또는 업데이트 (스냅샷 — 다음 호출 때 최신 집계로 다시 계산)
     */
    private ReportResponse generateReport(User user, LocalDate startDate, LocalDate endDate) {
        Aggregation agg = aggregateMoods(user, startDate, endDate);

        // 기존 리포트가 있으면 재사용, 없으면 새로 생성
        HealthReport report = reportRepository.findByUserAndStartDateAndEndDate(user, startDate, endDate)
                .orElseGet(() -> {
                    HealthReport newReport = HealthReport.builder()
                            .user(user)
                            .startDate(startDate)
                            .endDate(endDate)
                            .mostFrequentMood(agg.mostFrequentMood)
                            .activityScore(agg.totalEntries)
                            .build();
                    return reportRepository.save(newReport);
                });

        // 항상 최신 집계로 갱신 — 사용자가 일기를 추가/삭제하면 다음 호출 때 즉시 반영.
        report.updateStats(agg.mostFrequentMood, agg.totalEntries);

        return ReportResponse.from(report, agg.distribution, agg.totalEntries);
    }

    /**
     * 기간 내 모든 일기를 훑어 segment + final 기분을 합산한다.
     * - 일반 일기(segment) 한 건 = 1개 카운트
     * - 마무리 일기(finalMood not null) = 1개 카운트
     */
    private Aggregation aggregateMoods(User user, LocalDate startDate, LocalDate endDate) {
        List<Diary> diaries = diaryRepository
                .findByUserAndTargetDateBetweenOrderByTargetDateDesc(user, startDate, endDate);

        Map<String, Long> counts = new LinkedHashMap<>();
        for (MoodType mood : MoodType.values()) {
            counts.put(mood.name(), 0L);
        }

        int totalEntries = 0;
        for (Diary diary : diaries) {
            for (var seg : diary.getSegments()) {
                if (seg.getMoodSnapshot() != null) {
                    counts.merge(seg.getMoodSnapshot().name(), 1L, Long::sum);
                    totalEntries++;
                }
            }
            if (diary.getFinalMood() != null) {
                counts.merge(diary.getFinalMood().name(), 1L, Long::sum);
                totalEntries++;
            }
        }

        MoodType mostFrequentMood = counts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(e -> MoodType.valueOf(e.getKey()))
                .orElse(null);

        return new Aggregation(counts, totalEntries, mostFrequentMood);
    }

    private record Aggregation(
            Map<String, Long> distribution,
            int totalEntries,
            MoodType mostFrequentMood) {}

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
