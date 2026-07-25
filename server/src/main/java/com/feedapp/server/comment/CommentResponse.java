package com.feedapp.server.comment;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommentResponse {

    @NotNull
    private final Long id;
    @NotNull
    private final Long postId;
    @NotBlank
    private final String content;
    @NotBlank
    private final String author;
    @NotNull
    private final LocalDateTime createdAt;
}
