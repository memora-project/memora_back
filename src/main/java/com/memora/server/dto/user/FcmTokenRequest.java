package com.memora.server.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 등록 요청 DTO
 *
 * 프론트에서 Firebase로부터 발급받은 디바이스 토큰을 백엔드에 전달
 * { "fcmToken": "dK8x..." }
 */
@Getter
@NoArgsConstructor
public class FcmTokenRequest {

    private String fcmToken;
}
