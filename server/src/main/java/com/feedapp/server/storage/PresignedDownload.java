package com.feedapp.server.storage;

import jakarta.validation.constraints.NotBlank;

public record PresignedDownload(
        @NotBlank String url
) {
}
