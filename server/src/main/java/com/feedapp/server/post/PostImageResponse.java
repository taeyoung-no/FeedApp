package com.feedapp.server.post;

import jakarta.validation.constraints.NotBlank;

public record PostImageResponse(
        @NotBlank String key,
        @NotBlank String url
) {
}
