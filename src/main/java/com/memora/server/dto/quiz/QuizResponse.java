package com.memora.server.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 퀴즈 응답 DTO
 *
 * AI가 사진 + 기록을 분석하여 생성한 퀴즈
 * 응답 예시:
 * {
 *     "quizId": 37,
 *     "photoUrl": "/uploads/abc.jpg",
 *     "question": "이 사진에서 할머니가 드신 음식은 무엇일까요?",
 *     "choices": ["칼국수", "비빔밥", "냉면", "김치찌개"],
 *     "correctAnswer": "칼국수",
 *     "targetDate": "2026-05-10"
 * }
 */
@Getter
@AllArgsConstructor
public class QuizResponse {

    private Integer quizId;
    private String photoUrl;
    private String question;
    private List<String> choices;
    private String correctAnswer;
    private String targetDate;
}
