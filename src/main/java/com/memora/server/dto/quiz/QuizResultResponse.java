package com.memora.server.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 퀴즈 결과 응답 DTO
 *
 * { "correct": true, "message": "정답이에요! 대단하세요!", "correctAnswer": "대전 유성구 학하동" }
 */
@Getter
@AllArgsConstructor
public class QuizResultResponse {

    private Boolean correct;
    private String message;
    private String correctAnswer;
}
