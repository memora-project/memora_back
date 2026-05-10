package com.memora.server.controller;

import com.memora.server.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 역지오코딩 API
 *
 * 위도/경도 → 주소 변환
 * 회원가입 시 GPS 기반 주소 자동 입력에 사용
 * 인증 불필요 (회원가입 전 호출)
 */
@RestController
@RequestMapping("/api/v1/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeocodingService geocodingService;

    /**
     * 역지오코딩
     * GET /api/v1/geo/reverse?lat=36.3504&lng=127.3845
     *
     * 응답: { "address": "대전광역시 서구 둔산1동" }
     */
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverseGeocode(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {
        String address = geocodingService.reverseGeocode(lat, lng);
        if (address == null) {
            return ResponseEntity.ok(Map.of("address", ""));
        }
        return ResponseEntity.ok(Map.of("address", address));
    }
}
