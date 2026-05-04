package com.memora.server.exception;

/**
 * AI 요청 빈도 제한 초과 시 발생.
 * GlobalExceptionHandler가 HTTP 429 (Too Many Requests)로 매핑한다.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
