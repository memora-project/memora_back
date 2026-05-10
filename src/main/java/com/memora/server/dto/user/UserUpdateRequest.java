package com.memora.server.dto.user;

import com.memora.server.entity.enums.GenderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 내 정보 수정 요청 DTO
 *
 * PATCH 방식: 보내는 필드만 수정, 안 보내면 그대로 유지
 *
 * 예: 이름만 수정하고 싶으면
 * { "name": "새이름" }
 *
 * 예: 비상연락처 + 리포트 공유 동의 수정
 * { "emergencyContact": "010-1111-2222", "isReportShared": true }
 */
@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    private String name;
    private GenderType gender;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
    private String emergencyContact;
    private Boolean isReportShared;
    /** 호칭 (예: "할머니", "순자 어머니"). null이면 변경 안 함. */
    private String honorific;
    /**
     * 손주 얼굴 사진 url. 빈 문자열로 보내면 초기화 의미로 처리.
     * 별도 필드로 둬서 다른 프로필 필드 변경 없이 사진만 갱신 가능.
     */
    private String grandchildPhotoUrl;
}
