package com.memora.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 파일 업로드 서비스
 *
 * 이미지를 로컬 폴더에 저장하고 접근 URL을 반환
 * 나중에 S3로 교체할 때 이 클래스만 바꾸면 됨
 */
@Service
public class FileService {

    private final Path uploadDir;

    /**
     * 생성자: 업로드 폴더 생성
     * application.yaml의 file.upload-dir 값을 읽어옴
     * 폴더가 없으면 자동 생성
     */
    public FileService(@Value("${file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(this.uploadDir);
    }

    /**
     * 이미지 업로드
     *
     * 1. 파일 확장자 검증 (jpg, png, gif, webp만 허용)
     * 2. UUID로 고유한 파일명 생성 (중복 방지)
     * 3. 로컬 폴더에 저장
     * 4. 접근 URL 반환
     *
     * @param file 프론트에서 보낸 이미지 파일
     * @return 저장된 이미지의 접근 URL (예: /uploads/abc123.jpg)
     */
    public String uploadImage(MultipartFile file) throws IOException {
        // 1. 빈 파일 체크
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 2. MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // 3. 확장자 검증
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        if (!isImageExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, png, gif, webp만 가능)");
        }

        // 3. UUID로 고유 파일명 생성 (예: 550e8400-e29b-41d4-a716-446655440000.jpg)
        String savedFilename = UUID.randomUUID() + "." + extension;

        // 4. 파일 저장
        Path filePath = uploadDir.resolve(savedFilename);
        file.transferTo(filePath.toFile());

        // 5. 접근 URL 반환
        return "/uploads/" + savedFilename;
    }

    /**
     * 파일 확장자 추출
     * "photo.jpg" → "jpg"
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 이미지 확장자인지 확인
     */
    private boolean isImageExtension(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg")
                || extension.equals("png") || extension.equals("gif")
                || extension.equals("webp");
    }
}
