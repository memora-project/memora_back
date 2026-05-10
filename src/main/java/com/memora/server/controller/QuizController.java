package com.memora.server.controller;

import com.memora.server.dto.quiz.QuizAnswerRequest;
import com.memora.server.dto.quiz.QuizResponse;
import com.memora.server.dto.quiz.QuizResultResponse;
import com.memora.server.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 퀴즈 API 엔드포인트
 *
 * 사진 위치 기반 기억력 퀴즈 (알츠하이머 예방)
 * 토큰 필수
 */
@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * 오늘의 퀴즈 생성
     * GET /api/v1/quiz
     *
     * 사진 + 보기 4개 반환
     */
    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz() {
        Integer userId = getCurrentUserId();
        QuizResponse response = quizService.generateQuiz(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 정답 확인
     * POST /api/v1/quiz/answer
     *
     * 요청: { quizId: 37, answer: "대전 유성구 학하동" }
     * 응답: { correct: true, message: "정답이에요!", correctAnswer: "대전 유성구 학하동" }
     */
    @PostMapping("/answer")
    public ResponseEntity<QuizResultResponse> checkAnswer(@RequestBody QuizAnswerRequest request) {
        Integer userId = getCurrentUserId();
        QuizResultResponse response = quizService.checkAnswer(userId, request);
        return ResponseEntity.ok(response);
    }

    private Integer getCurrentUserId() {
        return (Integer) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
