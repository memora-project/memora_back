package com.memora.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CloudinaryImageStorage {
    private static final Pattern IMAGE_PATH = Pattern.compile("^/api/v1/files/images/([0-9a-fA-F-]{36})\\.(jpg|jpeg|png|gif|webp)$");
    private final Cloudinary cloudinary;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public CloudinaryImageStorage(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.required:false}") boolean required) {
        boolean any = !cloudName.isBlank() || !apiKey.isBlank() || !apiSecret.isBlank();
        if ((any && (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank())) || (required && !any)) {
            throw new IllegalStateException("Cloudinary cloud name, API key and API secret must all be configured");
        }
        cloudinary = any ? new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true)) : null;
    }

    public boolean enabled() {
        return cloudinary != null;
    }

    public String upload(byte[] bytes, Integer userId) throws IOException {
        String id = UUID.randomUUID().toString();
        String publicId = "memora/" + userId + "/" + id;
        @SuppressWarnings("rawtypes")
        Map result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "public_id", publicId, "type", "authenticated", "resource_type", "image"));
        String format = String.valueOf(result.get("format"));
        if (!format.matches("jpg|jpeg|png|gif|webp")) {
            throw new IOException("Unsupported Cloudinary image format: " + format);
        }
        return "/api/v1/files/images/" + id + "." + format;
    }

    public ImageData read(String path, Integer userId) throws IOException {
        return read(path, userId, false);
    }

    /**
     * AI Vision에는 원본 대신 최대 1024px JPEG를 전달한다. 사용자에게 표시하는
     * 원본은 건드리지 않으면서 Base64 요청 크기와 Render 메모리 사용량을 줄인다.
     */
    public ImageData readForAi(String path, Integer userId) throws IOException {
        return read(path, userId, true);
    }

    private ImageData read(String path, Integer userId, boolean optimizeForAi) throws IOException {
        if (cloudinary == null) throw new IOException("Cloudinary is not configured");
        Matcher matcher = IMAGE_PATH.matcher(path);
        if (!matcher.matches()) throw new IOException("Invalid image path");
        String id = matcher.group(1);
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid image ID", e);
        }
        String format = matcher.group(2);
        String publicId = "memora/" + userId + "/" + id;
        var urlBuilder = cloudinary.url().secure(true).resourceType("image")
                .type("authenticated").signed(true);
        if (optimizeForAi) {
            urlBuilder.transformation(new Transformation<>()
                    .width(1024).height(1024).crop("limit")
                    .quality("auto:good").fetchFormat("jpg"));
        }
        String signedUrl = urlBuilder.generate(publicId + "." + format);
        HttpRequest request = HttpRequest.newBuilder(URI.create(signedUrl))
                .timeout(Duration.ofSeconds(20)).GET().build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IOException("Image retrieval failed: HTTP " + response.statusCode());
            int maxBytes = optimizeForAi ? 3 * 1024 * 1024 : 10 * 1024 * 1024;
            if (response.body().length > maxBytes) throw new IOException("Image exceeds size limit");
            String contentType = optimizeForAi || format.equals("jpg") ? "image/jpeg" : "image/" + format;
            return new ImageData(response.body(), contentType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Image retrieval interrupted", e);
        }
    }

    public record ImageData(byte[] bytes, String contentType) {}
}
