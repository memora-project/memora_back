package com.memora.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
    private final CloudinaryImageStorage cloudinaryStorage;

    /**
     * 생성자: 업로드 폴더 생성
     * application.yaml의 file.upload-dir 값을 읽어옴
     * 폴더가 없으면 자동 생성
     */
    public FileService(@Value("${file.upload-dir:uploads}") String uploadDir,
                       CloudinaryImageStorage cloudinaryStorage) throws IOException {
        this.cloudinaryStorage = cloudinaryStorage;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath();
        if (!cloudinaryStorage.enabled()) Files.createDirectories(this.uploadDir);
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
    public String uploadImage(MultipartFile file, Integer userId) throws IOException {
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

        if (cloudinaryStorage.enabled()) {
            return cloudinaryStorage.upload(file.getBytes(), userId);
        }

        // 3. UUID로 고유 파일명 생성 (예: 550e8400-e29b-41d4-a716-446655440000.jpg)
        String savedFilename = UUID.randomUUID() + "." + extension;

        // 4. 파일 저장
        Path filePath = uploadDir.resolve(savedFilename);
        file.transferTo(filePath.toFile());

        // 5. 접근 URL 반환
        return "/uploads/" + savedFilename;
    }

    public CloudinaryImageStorage.ImageData readImage(String path, Integer userId) throws IOException {
        if (path.startsWith("/api/v1/files/images/")) {
            return cloudinaryStorage.read(path, userId);
        }
        Path localPath = resolveUploadedFile(uploadDir.toString(), path);
        if (localPath == null || !Files.isRegularFile(localPath)) throw new IOException("Image not found");
        String mimeType = Files.probeContentType(localPath);
        return new CloudinaryImageStorage.ImageData(Files.readAllBytes(localPath),
                mimeType == null ? "image/jpeg" : mimeType);
    }

    /**
     * 저장된 photoUrl(예: /uploads/abc.jpg)을 업로드 폴더 안의 실제 파일 경로로 변환.
     *
     * 경로 탈출(../), 절대 경로, 업로드 폴더 밖을 가리키는 값은 모두 null을 반환한다.
     * 사용자가 보낸 photoUrl을 그대로 파일 경로로 쓰면 서버의 임의 파일을 읽을 수 있기 때문.
     *
     * @param uploadDir 업로드 폴더 설정값 (file.upload-dir)
     * @param photoUrl  DB에 저장된 사진 URL
     * @return 업로드 폴더 안의 정규화된 경로, 안전하지 않으면 null
     */
    public static Path resolveUploadedFile(String uploadDir, String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return null;
        }
        String filename = photoUrl.startsWith("/uploads/")
                ? photoUrl.substring("/uploads/".length())
                : photoUrl;
        if (filename.isBlank()) {
            return null;
        }
        try {
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path candidate = Paths.get(filename);
            if (candidate.isAbsolute() || candidate.getRoot() != null) {
                return null;
            }
            Path resolved = root.resolve(candidate).normalize();
            if (!resolved.startsWith(root) || resolved.equals(root)) {
                return null;
            }
            return resolved;
        } catch (InvalidPathException e) {
            return null;
        }
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
