package com.memora.server.dto.auth;

import com.memora.server.entity.enums.GenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회원가입 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(max = 50, message = "아이디는 50자 이내로 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 4, max = 20, message = "비밀번호는 4~20자로 입력해주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 20, message = "이름은 20자 이내로 입력해주세요.")
    private String name;

    private GenderType gender;
    private LocalDate birthDate;
    private String phoneNumber;
    private String address;
    private String emergencyContact;
}
