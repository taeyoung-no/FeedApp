package com.feedapp.server.comment;

import java.time.LocalDateTime;
import java.util.List;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.post.Post;
import com.feedapp.server.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int CONTENT_MAX_LENGTH = 100;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<CommentResponse> findByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CommentResponse create(Long postId, String content, String author) {
        validateContent(content);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        Comment saved = commentRepository.save(
                new Comment(null, post, content, author, LocalDateTime.now())
        );
        return toResponse(saved);
    }

    private void validateContent(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("댓글 입력하세요");
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("너무 길어요");
        }
    }

    public CommentResponse update(Long id, String content, String username) {
        validateContent(content);
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("댓글 없음"));
        if (!comment.getAuthor().equals(username)) {
            throw new ForbiddenException("권한 없음");
        }
        Comment updated = commentRepository.save(
                new Comment(id, comment.getPost(), content, comment.getAuthor(), comment.getCreatedAt())
        );
        return toResponse(updated);
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
                comment.getPost().getId(),
                comment.getContent(),
                comment.getAuthor(),
                comment.getCreatedAt()
        );
    }
}
