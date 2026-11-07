package com.memora.server.service;

import com.cloudinary.Cloudinary;
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
        String signedUrl = cloudinary.url().secure(true).resourceType("image")
                .type("authenticated").signed(true).generate(publicId + "." + format);
        HttpRequest request = HttpRequest.newBuilder(URI.create(signedUrl))
                .timeout(Duration.ofSeconds(20)).GET().build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new IOException("Image retrieval failed: HTTP " + response.statusCode());
            if (response.body().length > 10 * 1024 * 1024) throw new IOException("Image exceeds 10 MB");
            String contentType = format.equals("jpg") ? "image/jpeg" : "image/" + format;
            return new ImageData(response.body(), contentType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Image retrieval interrupted", e);
        }
    }

    public record ImageData(byte[] bytes, String contentType) {}
}
