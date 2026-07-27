package com.feedapp.server.storage;

import jakarta.validation.constraints.NotBlank;

public record PresignedUpload(
        @NotBlank String key,
        @NotBlank String uploadUrl
) {
}
