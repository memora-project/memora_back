package com.memora.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * OpenRouter Chat Completions API와 통신하는 서비스
 *
 * 보안 원칙:
 *  - API 키는 서버 환경변수(OPENROUTER_API_KEY)에서만 읽는다.
 *    절대 클라이언트(앱)에 키나 프롬프트가 노출되면 안 된다.
 *
 * 책임:
 *  - 순수 HTTP 호출만 담당. Diary/Segment 같은 비즈니스 도메인은 모름.
 *  - 시스템/사용자 프롬프트를 받아 모델 응답 본문(string)을 반환할 뿐이다.
 *
 * 비즈니스 로직(ownership 검증, DB 저장, 프롬프트 조립)은 AiService가 처리한다.
 */
@Slf4j
@Service
public class OpenrouterService {

    private final RestClient restClient = RestClient.create();

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.endpoint}")
    private String endpoint;

    @Value("${openrouter.model}")
    private String model;

    /**
     * Chat Completions 호출. system + user 메시지 두 개로 구성된 프롬프트로
     * AI 응답 본문을 받는다.
     *
     * Throws:
     *  - IllegalArgumentException — 응답이 비어있거나 호출이 실패했을 때
     *    (GlobalExceptionHandler가 400으로 매핑 → 프론트에 메시지 그대로 전달)
     */
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0.8,
                "max_tokens", 1500,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        Map response;
        try {
            response = restClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    // OpenRouter는 다음 두 헤더로 호출원을 통계상 분류함.
                    .header("HTTP-Referer", "https://memora.app")
                    .header("X-Title", "Memora")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.error("OpenRouter 호출 실패", e);
            throw new IllegalArgumentException("AI 서비스 호출에 실패했습니다.");
        }

        if (response == null) {
            throw new IllegalArgumentException("AI 응답이 비어있습니다.");
        }

        List choices = (List) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("AI 응답에 choices가 없습니다.");
        }

        Map message = (Map) ((Map) choices.get(0)).get("message");
        String content = message != null ? (String) message.get("content") : null;
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("AI가 빈 응답을 반환했습니다.");
        }
        return content.trim();
    }
}
