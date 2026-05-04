package com.memora.server.scheduler;

import com.memora.server.entity.User;
import com.memora.server.repository.DiaryRepository;
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
    private final DiaryRepository diaryRepository;
    private final FcmService fcmService;

    /**
     * 매일 저녁 20:00 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendDiaryReminder() {
        log.info("일기 작성 리마인드 스케줄러 실행");

        LocalDate today = LocalDate.now();
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            // FCM 토큰이 없으면 알림을 보낼 수 없음
            if (user.getFcmToken() == null) continue;

            // 오늘 일기가 없는 사용자에게만 알림 발송
            boolean hasDiaryToday = diaryRepository
                    .findByUserAndTargetDate(user, today)
                    .isPresent();

            if (!hasDiaryToday) {
                fcmService.sendNotification(
                        user.getFcmToken(),
                        "오늘 하루는 어떠셨나요?",
                        "오늘의 기분을 기록해보세요. 소중한 하루를 남겨드릴게요."
                );
                log.info("리마인드 알림 발송: userId={}", user.getUserId());
            }
        }
    }
}
