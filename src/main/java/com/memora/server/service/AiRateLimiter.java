package com.memora.server.service;

import com.memora.server.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 AI 호출 횟수 제한기 (분당 10회).
 *
 * 메모리 기반 슬라이딩 윈도우. 단일 노드 환경에서만 정확하다.
 * 멀티 인스턴스로 배포할 경우 Redis/Bucket4j 등으로 교체 필요.
 *
 * 목적:
 *  - 사용자가 "다시 작성"을 빠르게 연타하거나 클라이언트 버그 루프로
 *    OpenRouter 비용이 폭주하는 것을 막는다.
 */
@Component
public class AiRateLimiter {

    private static final int MAX_PER_MINUTE = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<Long, Deque<Long>> userCalls = new ConcurrentHashMap<>();

    /**
     * 호출 1회를 등록. 윈도우 내 호출 수가 한도를 넘으면 예외.
     *
     * Throws:
     *  - RateLimitExceededException — 분당 한도 초과 시 (HTTP 429)
     */
    public void check(Long userId) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> calls = userCalls.computeIfAbsent(userId, k -> new ArrayDeque<>());

        synchronized (calls) {
            // 윈도우 밖 호출 기록 제거
            while (!calls.isEmpty() && now - calls.peekFirst() > WINDOW_MILLIS) {
                calls.pollFirst();
            }
            if (calls.size() >= MAX_PER_MINUTE) {
                throw new RateLimitExceededException(
                        "AI 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }
            calls.addLast(now);
        }
    }
}
