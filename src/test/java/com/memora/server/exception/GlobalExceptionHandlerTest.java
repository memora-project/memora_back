package com.memora.server.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 500 응답에 내부 예외 클래스명/메시지가 들어가지 않는지 확인.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 예상하지_못한_예외는_고정_문구로_500() {
        ResponseEntity<Map<String, String>> res = handler.handleException(
                new IllegalStateException("could not execute statement [insert into users ...]"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        String body = res.getBody().get("error");
        assertEquals(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE, body);
        assertFalse(body.contains("IllegalStateException"));
        assertFalse(body.contains("insert into"));
    }

    @Test
    void 잘못된_입력_400은_기존_메시지를_유지한다() {
        ResponseEntity<Map<String, String>> res =
                handler.handleIllegalArgument(new IllegalArgumentException("아이디를 찾을 수 없습니다."));

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("아이디를 찾을 수 없습니다.", res.getBody().get("error"));
    }
}
