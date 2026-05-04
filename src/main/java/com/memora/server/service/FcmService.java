package com.memora.server.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * FCM 푸시 알림 발송 서비스
 *
 * Firebase Cloud Messaging을 통해 사용자 디바이스에 푸시 알림을 보낸다.
 * fcmToken: 프론트에서 Firebase로부터 발급받아 서버에 저장한 디바이스 토큰
 */
@Slf4j
@Service
public class FcmService {

    /**
     * 단일 사용자에게 푸시 알림 발송
     *
     * @param fcmToken 대상 디바이스의 FCM 토큰
     * @param title    알림 제목
     * @param body     알림 내용
     */
    public void sendNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM 토큰이 없어서 알림을 보내지 못했습니다.");
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 알림 발송 성공: {}", response);
        } catch (Exception e) {
            log.error("FCM 알림 발송 실패: {}", e.getMessage());
        }
    }
}
