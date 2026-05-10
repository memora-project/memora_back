package com.memora.server.scheduler;

import com.memora.server.entity.Diary;
import com.memora.server.entity.UserSettings;
import com.memora.server.entity.enums.DiaryStatus;
import com.memora.server.repository.DiaryRepository;
import com.memora.server.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 자동 일기 완료 스케줄러
 *
 * 매 시간 정각에 실행.
 * 각 사용자의 autoCompleteTime 설정을 확인하여
 * 현재 시각이 설정 시간과 일치하면 전날 미완료 일기를 자동 COMPLETED 처리.
 *
 * 예: 사용자가 23:00으로 설정 → 23시 정각에 자동 완료
 *     기본값 00:00 → 자정에 자동 완료 (기존 동작과 동일)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryAutoCompleteScheduler {

    private final DiaryRepository diaryRepository;
    private final UserSettingsRepository settingsRepository;

    /**
     * 매 시간 정각 실행 (00:00, 01:00, ..., 23:00)
     * 사용자별 autoCompleteTime에 해당하는 시간에만 처리
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    @Transactional
    public void autoCompleteDiaries() {
        LocalTime currentHour = LocalTime.of(LocalTime.now().getHour(), 0);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        log.info("자동 완료 스케줄러 실행 - 현재 시각: {}", currentHour);

        // 현재 시각에 autoCompleteTime이 설정된 사용자의 미완료 일기 처리
        List<UserSettings> matchingSettings = settingsRepository.findAll().stream()
                .filter(s -> LocalTime.of(s.getAutoCompleteTime().getHour(), 0).equals(currentHour))
                .toList();

        for (UserSettings settings : matchingSettings) {
            List<Diary> incompleteDiaries = diaryRepository
                    .findByStatusAndTargetDate(DiaryStatus.IN_PROGRESS, yesterday);

            for (Diary diary : incompleteDiaries) {
                if (diary.getUser().getUserId().equals(settings.getUserId())) {
                    diary.complete();
                    log.info("일기 자동 완료: diaryId={}, userId={}, 설정시간={}",
                            diary.getDiaryId(), settings.getUserId(), settings.getAutoCompleteTime());
                }
            }
        }
    }
}
