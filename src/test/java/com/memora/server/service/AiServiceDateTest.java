package com.memora.server.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceDateTest {

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 9);

    @Test
    void replacesAiPlaceholderWithExactTargetDate() {
        String result = AiService.withExactDiaryDate(
                TARGET_DATE,
                "202X년 X월 XX일\n오늘은 즐거운 하루였다."
        );

        assertThat(result).isEqualTo("2026년 11월 9일\n오늘은 즐거운 하루였다.");
    }

    @Test
    void replacesWrongConcreteDateWithExactTargetDate() {
        String result = AiService.withExactDiaryDate(
                TARGET_DATE,
                "2025년 1월 2일 (목요일)\n오늘은 산책을 했다."
        );

        assertThat(result).isEqualTo("2026년 11월 9일\n오늘은 산책을 했다.");
    }

    @Test
    void prependsExactDateWhenAiReturnsOnlyBody() {
        String result = AiService.withExactDiaryDate(TARGET_DATE, "오늘은 가족을 만났다.");

        assertThat(result).isEqualTo("2026년 11월 9일\n오늘은 가족을 만났다.");
    }
}
