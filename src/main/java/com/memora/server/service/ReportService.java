package com.memora.server.service;

import com.memora.server.dto.report.ReportResponse;
import com.memora.server.entity.Diary;
import com.memora.server.entity.HealthReport;
import com.memora.server.entity.User;
import com.memora.server.entity.enums.DiaryStatus;
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
import java.util.stream.Collectors;

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
        List<Diary> completedDiaries = diaryRepository
                .findByUserAndStatusAndTargetDateBetween(user, DiaryStatus.COMPLETED,
                        report.getStartDate(), report.getEndDate());

        Map<String, Long> moodDistribution = completedDiaries.stream()
                .filter(d -> d.getFinalMood() != null)
                .collect(Collectors.groupingBy(d -> d.getFinalMood().name(), Collectors.counting()));

        Map<String, Long> fullDistribution = new LinkedHashMap<>();
        for (MoodType mood : MoodType.values()) {
            fullDistribution.put(mood.name(), moodDistribution.getOrDefault(mood.name(), 0L));
        }

        return ReportResponse.from(report, fullDistribution, completedDiaries.size());
    }

    /**
     * 리포트 생성/갱신 공통 로직
     *
     * 1. 해당 기간의 완료된 일기 조회
     * 2. 기분 분포 계산 (GREAT: 3, CALM: 2 ...)
     * 3. 가장 빈번한 기분 결정
     * 4. 활동 점수 = 완료된 일기 수
     * 5. DB에 저장 또는 업데이트
     */
    private ReportResponse generateReport(User user, LocalDate startDate, LocalDate endDate) {
        // 1. 해당 기간의 완료된 일기 조회
        List<Diary> completedDiaries = diaryRepository
                .findByUserAndStatusAndTargetDateBetween(user, DiaryStatus.COMPLETED, startDate, endDate);

        // 2. 기분 분포 계산
        Map<String, Long> moodDistribution = completedDiaries.stream()
                .filter(d -> d.getFinalMood() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getFinalMood().name(),
                        Collectors.counting()
                ));

        // 모든 기분 타입을 포함하되 0으로 초기화
        Map<String, Long> fullDistribution = new LinkedHashMap<>();
        for (MoodType mood : MoodType.values()) {
            fullDistribution.put(mood.name(), moodDistribution.getOrDefault(mood.name(), 0L));
        }

        // 3. 가장 빈번한 기분
        MoodType mostFrequentMood = moodDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> MoodType.valueOf(e.getKey()))
                .orElse(null);

        // 4. 활동 점수 = 완료된 일기 수
        int activityScore = completedDiaries.size();

        // 5. 기존 리포트가 있으면 가져오고, 없으면 새로 생성
        HealthReport report = reportRepository.findByUserAndStartDateAndEndDate(user, startDate, endDate)
                .orElseGet(() -> {
                    HealthReport newReport = HealthReport.builder()
                            .user(user)
                            .startDate(startDate)
                            .endDate(endDate)
                            .mostFrequentMood(mostFrequentMood)
                            .activityScore(activityScore)
                            .build();
                    return reportRepository.save(newReport);
                });

        return ReportResponse.from(report, fullDistribution, activityScore);
    }

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
