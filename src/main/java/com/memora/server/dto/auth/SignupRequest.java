package com.memora.server.dto.auth;

import com.memora.server.entity.enums.GenderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회원가입 요청 DTO
 *
 * 프론트가 회원가입할 때 보내는 데이터:
 * {
 *     "loginId": "memora123",
 *     "password": "1234",
 *     "name": "홍길동",
 *     "gender": "MALE",
 *     "birthDate": "1955-03-15",
 *     "phoneNumber": "010-1234-5678",
 *     "address": "대전시 유성구",
 *     "emergencyContact": "010-9876-5432"
 * }
 */
@Getter
@NoArgsConstructor
public class SignupRequest {

    private String loginId;
    private String password;
    private String name;
    private GenderType gender;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
    private String emergencyContact;
}
