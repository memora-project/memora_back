package com.memora.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 역지오코딩 서비스
 *
 * 카카오 로컬 API를 사용하여 위도/경도 → 주소(동 단위)로 변환
 * 예: (36.3504, 127.3845) → "대전 유성구 학하동"
 */
@Slf4j
@Service
public class GeocodingService {

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.rest-api-key}")
    private String kakaoApiKey;

    /**
     * 위도/경도를 주소로 변환
     *
     * @param latitude  위도
     * @param longitude 경도
     * @return 주소 문자열 (예: "대전 유성구 학하동"), 변환 실패 시 null
     */
    public String reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        try {
            Map response = restClient.get()
                    .uri("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x={lng}&y={lat}",
                            longitude, latitude)
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            List documents = (List) response.get("documents");
            if (documents == null || documents.isEmpty()) return null;

            // 법정동 주소(H) 우선, 없으면 행정동(B)
            for (Object doc : documents) {
                Map document = (Map) doc;
                String regionType = (String) document.get("region_type");
                if ("H".equals(regionType)) {
                    return buildAddress(document);
                }
            }

            // H가 없으면 첫 번째 결과 사용
            return buildAddress((Map) documents.get(0));

        } catch (Exception e) {
            log.error("역지오코딩 실패: lat={}, lng={}", latitude, longitude, e);
            return null;
        }
    }

    /**
     * 카카오 응답에서 "시/도 구/군 동" 형태로 주소 조립
     */
    private String buildAddress(Map document) {
        String region1 = (String) document.get("region_1depth_name"); // 시/도
        String region2 = (String) document.get("region_2depth_name"); // 구/군
        String region3 = (String) document.get("region_3depth_name"); // 동/면/리

        StringBuilder sb = new StringBuilder();
        if (region1 != null) sb.append(region1);
        if (region2 != null) sb.append(" ").append(region2);
        if (region3 != null) sb.append(" ").append(region3);

        return sb.toString().trim();
    }
}
