package com.memora.server.dto.user;

import com.memora.server.entity.User;
import com.memora.server.entity.enums.GenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 내 정보 조회 응답 DTO
 *
 * User 엔티티에서 프론트에 필요한 정보만 골라서 보내줌
 * (비밀번호, refreshToken 같은 민감 정보는 제외!)
 *
 * 응답 예시:
 * {
 *     "userId": 1,
 *     "loginId": "memora123",
 *     "name": "홍길동",
 *     "gender": "MALE",
 *     "birthDate": "1955-03-15",
 *     "phoneNumber": "010-1234-5678",
 *     "address": "대전시 유성구",
 *     "emergencyContact": "010-9876-5432",
 *     "isReportShared": false,
 *     "isKakaoUser": false
 * }
 */
@Getter
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String loginId;
    private String name;
    private GenderType gender;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
    private String emergencyContact;
    private Boolean isReportShared;
    private Boolean isKakaoUser;

    /**
     * User 엔티티 → UserResponse 변환
     * Entity를 직접 노출하지 않고 DTO로 변환하는 패턴
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getGender(),
                user.getBirthDate(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getEmergencyContact(),
                user.getIsReportShared(),
                user.getKakaoId() != null  // kakaoId가 있으면 카카오 유저
        );
    }
}
