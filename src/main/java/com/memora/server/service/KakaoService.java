package com.memora.server.service;

import com.memora.server.entity.enums.GenderType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 카카오 API와 통신하는 서비스
 *
 * 웹 로그인 흐름:
 * 1. 프론트가 카카오 로그인 페이지로 보냄
 * 2. 사용자가 로그인하면 카카오가 "인가코드(code)"를 줌
 * 3. 백엔드가 인가코드로 카카오 Access Token을 받음
 * 4. 카카오 Access Token으로 사용자 정보를 가져옴
 */
@Service
public class KakaoService {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient = createRestClient();

    @Value("${kakao.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    /**
     * 카카오 서버가 응답하지 않을 때 요청 스레드가 오래 묶이지 않도록 연결/응답 시간을 제한한다.
     */
    private static RestClient createRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 인가코드 → 카카오 Access Token 교환
     *
     * 카카오에 POST 요청:
     * grant_type=authorization_code
     * client_id=REST API 키
     * redirect_uri=프론트 리다이렉트 주소
     * code=인가코드
     *
     * 카카오 응답: { "access_token": "xxx", ... }
     */
    public String getAccessToken(String code, String redirectUri) {
        String body = "grant_type=authorization_code"
                + "&client_id=" + restApiKey
                + "&client_secret=" + clientSecret
                + "&redirect_uri=" + redirectUri
                + "&code=" + code;

        Map response = restClient.post()
                .uri(KAKAO_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw new IllegalArgumentException("카카오 토큰을 가져올 수 없습니다.");
        }

        return (String) response.get("access_token");
    }

    /**
     * 카카오 Access Token으로 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        Map response = restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalArgumentException("카카오 사용자 정보를 가져올 수 없습니다.");
        }

        KakaoUserInfo info = parseUserInfo(response);
        org.slf4j.LoggerFactory.getLogger(KakaoService.class).info(
                "Kakao profile availability: name={}, gender={}, birthDate={}, phone={}, additionalConsentScopes={}",
                !"카카오 사용자".equals(info.getName()), info.getGender() != null,
                info.getBirthDate() != null, info.getPhoneNumber() != null,
                info.getAdditionalConsentScopes());
        return info;
    }

    static KakaoUserInfo parseUserInfo(Map<?, ?> response) {
        String kakaoId = String.valueOf(response.get("id"));
        Map<?, ?> account = response.get("kakao_account") instanceof Map<?, ?> value ? value : Map.of();
        Map<?, ?> profile = account.get("profile") instanceof Map<?, ?> value ? value : Map.of();
        Map<?, ?> properties = response.get("properties") instanceof Map<?, ?> value ? value : Map.of();

        String nickname = nonBlank(profile.get("nickname"));
        if (nickname == null) nickname = nonBlank(properties.get("nickname"));
        String name = nonBlank(account.get("name"));
        if (name == null) name = nickname;
        if (name == null) name = "카카오 사용자";
        if (name.length() > 20) name = name.substring(0, 20);

        GenderType gender = switch (String.valueOf(account.get("gender"))) {
            case "male" -> GenderType.MALE;
            case "female" -> GenderType.FEMALE;
            default -> null;
        };

        LocalDate birthDate = null;
        String birthyear = nonBlank(account.get("birthyear"));
        String birthday = nonBlank(account.get("birthday"));
        // The app stores a Gregorian date. A lunar birthday cannot be converted without more data.
        if (birthyear != null && birthday != null && birthyear.matches("\\d{4}")
                && birthday.matches("\\d{4}") && "SOLAR".equals(account.get("birthday_type"))) {
            try {
                birthDate = LocalDate.of(Integer.parseInt(birthyear),
                        Integer.parseInt(birthday.substring(0, 2)),
                        Integer.parseInt(birthday.substring(2, 4)));
            } catch (java.time.DateTimeException ignored) {
                // Missing or invalid Kakao birthday remains for the user to enter.
            }
        }

        String phoneNumber = null;
        String kakaoPhone = nonBlank(account.get("phone_number"));
        if (kakaoPhone != null) {
            String digits = kakaoPhone.replaceAll("\\D", "");
            if (digits.startsWith("82")) {
                digits = digits.substring(2);
                if (!digits.startsWith("0")) digits = "0" + digits;
            }
            if (digits.matches("0\\d{9,10}")) phoneNumber = digits;
        }

        List<String> additionalConsentScopes = new ArrayList<>();
        if (nonBlank(account.get("name")) == null && Boolean.TRUE.equals(account.get("name_needs_agreement"))) {
            additionalConsentScopes.add("name");
        }
        if (nickname == null && Boolean.TRUE.equals(account.get("profile_nickname_needs_agreement"))) {
            additionalConsentScopes.add("profile_nickname");
        }
        if (gender == null && Boolean.TRUE.equals(account.get("gender_needs_agreement"))) {
            additionalConsentScopes.add("gender");
        }
        if (birthyear == null && Boolean.TRUE.equals(account.get("birthyear_needs_agreement"))) {
            additionalConsentScopes.add("birthyear");
        }
        if (birthday == null && Boolean.TRUE.equals(account.get("birthday_needs_agreement"))) {
            additionalConsentScopes.add("birthday");
        }
        if (phoneNumber == null && Boolean.TRUE.equals(account.get("phone_number_needs_agreement"))) {
            additionalConsentScopes.add("phone_number");
        }

        return new KakaoUserInfo(kakaoId, name, gender, birthDate, phoneNumber, additionalConsentScopes);
    }

    private static String nonBlank(Object value) {
        if (!(value instanceof String text) || text.isBlank()) return null;
        return text.trim();
    }

    @Getter
    public static class KakaoUserInfo {
        private final String kakaoId;
        private final String name;
        private final GenderType gender;
        private final LocalDate birthDate;
        private final String phoneNumber;
        private final List<String> additionalConsentScopes;

        public KakaoUserInfo(String kakaoId, String name, GenderType gender,
                             LocalDate birthDate, String phoneNumber) {
            this(kakaoId, name, gender, birthDate, phoneNumber, List.of());
        }

        public KakaoUserInfo(String kakaoId, String name, GenderType gender,
                             LocalDate birthDate, String phoneNumber, List<String> additionalConsentScopes) {
            this.kakaoId = kakaoId;
            this.name = name;
            this.gender = gender;
            this.birthDate = birthDate;
            this.phoneNumber = phoneNumber;
            this.additionalConsentScopes = List.copyOf(additionalConsentScopes);
        }
    }
}
