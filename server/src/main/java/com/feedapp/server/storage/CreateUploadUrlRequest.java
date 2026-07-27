package com.feedapp.server.storage;

import jakarta.validation.constraints.NotBlank;

public record CreateUploadUrlRequest(
        @NotBlank String contentType
) {
}
