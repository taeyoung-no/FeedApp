package com.feedapp.server.storage;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ImageStorage imageStorage;

    public StoredImage upload(InputStream content, String contentType, long contentLength) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("허용되지 않는 형식");
        }
        if (contentLength <= 0) {
            throw new IllegalArgumentException("사진이 없는 듯");
        }

        String key = "posts/" + UUID.randomUUID() + extensionOf(contentType);
        return imageStorage.upload(key, content, contentType, contentLength);
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("삭제할 사진이 없어요");
        }
        imageStorage.delete(key);
    }

    private static String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("허용되지 않는 형식");
        };
    }
}
