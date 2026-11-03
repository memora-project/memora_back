package com.memora.server.service;

/**
 * AI 모델 공급자와 통신하는 공통 인터페이스.
 */
public interface AiClient {

    String chat(String systemPrompt, String userPrompt);

    String chatWithVision(String systemPrompt, String userPrompt,
                          String base64Image, String mimeType);
}
