package com.memora.server.scheduler;

import com.memora.server.entity.User;
import com.memora.server.repository.UserRepository;
import com.memora.server.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 일기 작성 리마인드 스케줄러
 *
 * 매일 저녁 8시에 실행
 * 오늘 일기를 작성하지 않은 사용자에게 푸시 알림을 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryReminderScheduler {

    private final UserRepository userRepository;
    private final FcmService fcmService;

    /**
     * 매일 저녁 20:00 실행
     *
     * 한 번의 쿼리로 "FCM 토큰 있고 + 오늘 일기 없는" 사용자만 조회 (N+1 방지)
     */
    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    public void sendDiaryReminder() {
        log.info("일기 작성 리마인드 스케줄러 실행");

        LocalDate today = LocalDate.now();
        List<User> usersToRemind = userRepository.findUsersWithoutDiaryOnDate(today);

        for (User user : usersToRemind) {
            fcmService.sendNotification(
                    user.getFcmToken(),
                    "오늘 하루는 어떠셨나요?",
                    "오늘의 기분을 기록해보세요. 소중한 하루를 남겨드릴게요."
            );
            log.info("리마인드 알림 발송: userId={}", user.getUserId());
        }

        log.info("리마인드 알림 발송 완료: {}명", usersToRemind.size());
    }
}
