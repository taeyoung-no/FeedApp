package com.feedapp.server.comment;

import java.time.LocalDateTime;
import java.util.List;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
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

    public CommentResponse create(Long postId, String content, String author) {
        Comment saved = commentRepository.save(
                new Comment(null, postId, content, author, LocalDateTime.now())
        );
        return toResponse(saved);
    }

    public void delete(Long id, String username) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("댓글 없음"));
        if (!comment.getAuthor().equals(username)) {
            throw new ForbiddenException("권한 없음");
        }
        commentRepository.delete(comment);
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


