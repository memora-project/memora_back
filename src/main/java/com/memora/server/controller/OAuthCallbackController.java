package com.memora.server.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kakao OAuth redirects the embedded browser to this URL after issuing a code.
 * The Android app consumes the code from the URL, while this endpoint prevents
 * the WebView from displaying Spring Security's 403 page during token exchange.
 */
@RestController
public class OAuthCallbackController {

    @GetMapping(value = "/oauth/kakao", produces = MediaType.TEXT_HTML_VALUE)
    public String kakaoCallback() {
        return """
                <!doctype html>
                <html lang="ko">
                <head><meta charset="utf-8"><title>Memora</title></head>
                <body>카카오 로그인 정보를 확인하고 있습니다.</body>
                </html>
                """;
    }
}
