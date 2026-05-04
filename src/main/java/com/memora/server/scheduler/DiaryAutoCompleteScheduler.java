package com.memora.server.scheduler;

import com.memora.server.entity.Diary;
import com.memora.server.entity.enums.DiaryStatus;
import com.memora.server.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 자정 자동 일기 완료 스케줄러
 *
 * 매일 자정(00:00)에 실행
 * 전날 작성했지만 완료 처리하지 않은 일기(IN_PROGRESS)를
 * 자동으로 COMPLETED 상태로 변경한다.
 *
 * 노년층 사용자가 완료 버튼을 깜빡할 수 있으므로
 * 하루가 지나면 자동 완료 처리하여 리포트에 반영되도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryAutoCompleteScheduler {

    private final DiaryRepository diaryRepository;

    /**
     * 매일 자정(00:00) 실행
     * 전날(어제) 날짜의 IN_PROGRESS 일기를 COMPLETED로 변경
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void autoCompleteDiaries() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("자정 자동 완료 스케줄러 실행 - 대상 날짜: {}", yesterday);

        List<Diary> incompleteDiaries = diaryRepository
                .findByStatusAndTargetDate(DiaryStatus.IN_PROGRESS, yesterday);

        for (Diary diary : incompleteDiaries) {
            diary.complete();
            log.info("일기 자동 완료 처리: diaryId={}, userId={}",
                    diary.getDiaryId(), diary.getUser().getUserId());
        }

        log.info("자동 완료 처리된 일기 수: {}", incompleteDiaries.size());
    }
}
