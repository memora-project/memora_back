package com.memora.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 *
 * Gmail SMTP를 통해 인증번호 이메일 발송
 * 비밀번호 재설정 시 본인 확인용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 인증번호 이메일 발송
     *
     * @param to   수신자 이메일 (loginId)
     * @param code 6자리 인증번호
     */
    public void sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[Memora] 비밀번호 재설정 인증번호");
            message.setText("안녕하세요, Memora입니다.\n\n"
                    + "비밀번호 재설정 인증번호: " + code + "\n\n"
                    + "인증번호는 5분간 유효합니다.\n"
                    + "본인이 요청하지 않았다면 이 메일을 무시해주세요.");

            mailSender.send(message);
            log.info("인증번호 이메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("인증번호 이메일 발송 실패: {}", to, e);
            throw new IllegalArgumentException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
