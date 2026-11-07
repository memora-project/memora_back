package com.memora.server.controller;

import com.memora.server.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 파일 업로드 API 엔드포인트
 *
 * 이미지 업로드 후 URL을 반환
 * 토큰 필수
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 이미지 업로드
     * POST /api/v1/files/images
     *
     * 요청: multipart/form-data (key: "file")
     * 응답: { "url": "/uploads/abc123.jpg" }
     *
     * @RequestParam("file"): form-data에서 "file" 키로 보낸 파일을 받음
     */
    @PostMapping("/images")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String url = fileService.uploadImage(file, userId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var image = fileService.readImage("/api/v1/files/images/" + filename, userId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.bytes());
    }
}
