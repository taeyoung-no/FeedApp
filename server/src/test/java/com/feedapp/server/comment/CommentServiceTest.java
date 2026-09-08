package com.feedapp.server.comment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.post.Post;
import com.feedapp.server.post.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    CommentRepository commentRepository;

    @Mock
    PostRepository postRepository;

    @InjectMocks
    CommentService commentService;

    @Test
    @DisplayName("게시글이 없으면 댓글 작성 실패")
    void createWhenPostNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(1L, "content", "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 비어 있으면 작성 실패")
    void createWhenContentEmpty() {
        assertThatThrownBy(() -> commentService.create(1L, "", "author"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 입력하세요");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 100자를 넘으면 작성 실패")
    void createWhenContentTooLong() {
        assertThatThrownBy(() -> commentService.create(1L, "a".repeat(101), "author"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("너무 길어요");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 없으면 수정 실패")
    void updateCommentWhenNotFound() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(1L, "newContent", "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("댓글 없음");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("작성자가 아니면 수정 실패")
    void updateCommentWhenNotAuthor() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, "author")));

        assertThatThrownBy(() -> commentService.update(1L, "newContent", "other"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 없으면 삭제 실패")
    void deleteCommentWhenNotFound() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(1L, "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("댓글 없음");

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 실패")
    void deleteCommentWhenNotAuthor() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment(1L, "author")));

        assertThatThrownBy(() -> commentService.delete(1L, "other"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    private static Comment comment(Long id, String author) {
        return new Comment(
                id,
                new Post(1L, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), new ArrayList<>()),
                "content",
                author,
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
    }
}
