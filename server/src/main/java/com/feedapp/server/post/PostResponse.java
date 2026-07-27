package com.feedapp.server.post;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostResponse {

    @NotNull
    private final Long id;
    @NotBlank
    private final String title;
    @NotBlank
    private final String content;
    @NotBlank
    private final String author;
    @NotNull
    private final LocalDateTime createdAt;
    @NotNull
    private final List<PostImageResponse> images;
}
