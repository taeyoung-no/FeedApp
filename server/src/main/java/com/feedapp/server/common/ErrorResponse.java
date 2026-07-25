package com.feedapp.server.common;

import jakarta.validation.constraints.NotBlank;

public record ErrorResponse(
        @NotBlank String message
) {
}
