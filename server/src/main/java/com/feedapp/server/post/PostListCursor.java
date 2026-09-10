package com.feedapp.server.post;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

record PostListCursor(LocalDateTime createdAt, long id, boolean previous) {

    String encode() {
        String raw = createdAt + "|" + id + "|" + (previous ? "p" : "n");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static PostListCursor decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 3);
            if (parts.length != 3 || (!parts[2].equals("p") && !parts[2].equals("n"))) {
                throw new IllegalArgumentException("잘못된 커서");
            }
            return new PostListCursor(
                    LocalDateTime.parse(parts[0]),
                    Long.parseLong(parts[1]),
                    parts[2].equals("p")
            );
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("잘못된 커서");
        }
    }
}
