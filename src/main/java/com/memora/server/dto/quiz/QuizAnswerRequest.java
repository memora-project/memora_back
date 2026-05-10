package com.memora.server.dto.quiz;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀴즈 정답 확인 요청 DTO
 *
 * { "quizId": 37, "answer": "칼국수", "correctAnswer": "칼국수" }
 */
@Getter
@NoArgsConstructor
public class QuizAnswerRequest {

    private Integer quizId;
    private String answer;
    private String correctAnswer;
}
