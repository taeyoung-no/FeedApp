package com.feedapp.server.post;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostRequest(
        @NotBlank String title,
        @NotBlank String content,
        List<String> imageKeys
) {
}
