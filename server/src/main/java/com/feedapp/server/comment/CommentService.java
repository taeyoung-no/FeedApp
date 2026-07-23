package com.feedapp.server.comment;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public List<CommentResponse> findByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getContent(),
                comment.getAuthor(),
                comment.getCreatedAt()
        );
    }
}
