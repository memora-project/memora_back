package com.memora.server.service;

import com.memora.server.entity.enums.GenderType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KakaoServiceTest {

    @Test
    void readsConsentedAccountFieldsAndNormalizesKoreanPhone() {
        var info = KakaoService.parseUserInfo(Map.of(
                "id", 12345,
                "kakao_account", Map.of(
                        "name", "Test User",
                        "profile", Map.of("nickname", "Nickname"),
                        "gender", "female",
                        "birthyear", "1958",
                        "birthday", "0324",
                        "birthday_type", "SOLAR",
                        "phone_number", "+82 10-1234-5678")));

        assertEquals("12345", info.getKakaoId());
        assertEquals("Test User", info.getName());
        assertEquals(GenderType.FEMALE, info.getGender());
        assertEquals(LocalDate.of(1958, 3, 24), info.getBirthDate());
        assertEquals("01012345678", info.getPhoneNumber());
    }

    @Test
    void leavesUnconsentedOrLunarFieldsForManualEntry() {
        var info = KakaoService.parseUserInfo(Map.of(
                "id", 7,
                "kakao_account", Map.of(
                        "profile", Map.of("nickname", "Nickname"),
                        "birthyear", "1958",
                        "birthday", "0324",
                        "birthday_type", "LUNAR")));

        assertEquals("Nickname", info.getName());
        assertNull(info.getGender());
        assertNull(info.getBirthDate());
        assertNull(info.getPhoneNumber());
    }
}
