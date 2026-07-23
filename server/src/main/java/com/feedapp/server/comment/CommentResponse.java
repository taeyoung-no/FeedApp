package com.feedapp.server.comment;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommentResponse {

    private final Long id;
    private final Long postId;
    private final String content;
    private final String author;
    private final LocalDateTime createdAt;
}
