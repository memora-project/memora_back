package com.memora.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent API와 통신하는 서비스.
 */
@Slf4j
@Service
public class GeminiService implements AiClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient = createRestClient();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.endpoint}")
    private String endpoint;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.fallback-model}")
    private String fallbackModel;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, List.of(Map.of("text", userPrompt)));
    }

    @Override
    public String chatWithVision(String systemPrompt, String userPrompt,
                                 String base64Image, String mimeType) {
        if (base64Image == null || base64Image.isBlank()) {
            return chat(systemPrompt, userPrompt);
        }

        String resolvedMimeType = mimeType == null || mimeType.isBlank()
                ? "image/jpeg"
                : mimeType;

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", userPrompt));
        parts.add(Map.of(
                "inlineData", Map.of(
                        "mimeType", resolvedMimeType,
                        "data", base64Image
                )
        ));
        return generate(systemPrompt, parts);
    }

    private String generate(String systemPrompt, List<Map<String, Object>> userParts) {
        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", userParts
                )),
                "generationConfig", Map.of(
                        "maxOutputTokens", 512,
                        "thinkingConfig", Map.of(
                                "thinkingLevel", "low"
                        )
                )
        );

        IllegalArgumentException lastFailure = null;
        for (String candidateModel : candidateModels()) {
            try {
                Map<?, ?> response = request(candidateModel, requestBody);
                String text = extractText(response);
                if (text == null || text.isBlank()) {
                    throw new IllegalArgumentException("AI가 빈 응답을 반환했습니다.");
                }
                return text.trim();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                log.warn("Gemini 모델 호출 실패: model={}, status={}, body={}",
                        candidateModel, status, e.getResponseBodyAsString());

                if (status != 404 && status != 429 && status != 503) {
                    throw toClientException(e);
                }
                lastFailure = toClientException(e);
            } catch (ResourceAccessException e) {
                log.warn("Gemini 모델 응답 지연: model={}, timeout={}s",
                        candidateModel, RESPONSE_TIMEOUT.toSeconds());
                lastFailure = new IllegalArgumentException(
                        "Gemini 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
            } catch (RestClientException e) {
                log.error("Gemini 호출 실패", e);
                throw new IllegalArgumentException("Gemini 서버에 연결할 수 없습니다.");
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IllegalArgumentException("사용 가능한 Gemini 모델이 설정되지 않았습니다.");
    }

    private static RestClient createRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(RESPONSE_TIMEOUT);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private Map<?, ?> request(String candidateModel, Map<String, Object> requestBody) {
        return restClient.post()
                .uri(endpoint, candidateModel)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);
    }

    private List<String> candidateModels() {
        if (fallbackModel == null || fallbackModel.isBlank() || fallbackModel.equals(model)) {
            return List.of(model);
        }
        return List.of(model, fallbackModel);
    }

    private IllegalArgumentException toClientException(RestClientResponseException e) {
        return switch (e.getStatusCode().value()) {
            case 401, 403 -> new IllegalArgumentException(
                    "Gemini API 키 또는 프로젝트 권한을 확인해주세요.");
            case 404 -> new IllegalArgumentException(
                    "현재 사용할 수 있는 Gemini 모델이 없습니다.");
            case 429 -> new IllegalArgumentException(
                    "Gemini 무료 사용 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
            case 503 -> new IllegalArgumentException(
                    "Gemini 서버 사용량이 많습니다. 잠시 후 다시 시도해주세요.");
            default -> new IllegalArgumentException("AI 서비스 호출에 실패했습니다.");
        };
    }

    private String extractText(Map<?, ?> response) {
        if (response == null) {
            return null;
        }

        Object candidatesValue = response.get("candidates");
        if (!(candidatesValue instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        if (!(candidates.get(0) instanceof Map<?, ?> candidate)) {
            return null;
        }
        if (!(candidate.get("content") instanceof Map<?, ?> content)) {
            return null;
        }
        if (!(content.get("parts") instanceof List<?> parts)) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (Object partValue : parts) {
            if (partValue instanceof Map<?, ?> part && part.get("text") instanceof String text) {
                result.append(text);
            }
        }
        return result.toString();
    }
}
