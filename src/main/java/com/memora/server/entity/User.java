package com.memora.server.entity;

import com.memora.server.entity.enums.GenderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 무분별한 객체 생성을 막기 위해 보호
@AllArgsConstructor // 모든 필드를 포함한 생성자 (Builder를 위해 필요)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String name;

    // gender_type은 DB의 ENUM이므로 String으로 받거나 별도의 Enum 클래스를 연결합니다.
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GenderType gender;

    @Column(nullable = false)
    private LocalDate birthDate;

    private String address;

    @Column(length = 15)
    private String emergencyContact;

    @Builder.Default
    @Column(updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    // 빌더 패턴이나 생성자를 추가해서 데이터를 넣을 수 있게 만들 수 있습니다.
}