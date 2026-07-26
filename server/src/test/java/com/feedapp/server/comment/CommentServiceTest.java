package com.feedapp.server.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
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
    @DisplayName("댓글이 있으면 생성 시각 내림차순 목록을 반환")
    void findByPostId() {
        final Long postId = 1L;
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(postId)).thenReturn(List.of(
                new Comment(1L, postId, "content1", "author1", createdAt1),
                new Comment(2L, postId, "content2", "author2", createdAt2)
        ));

        final List<CommentResponse> result = commentService.findByPostId(postId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getPostId()).isEqualTo(postId);
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getAuthor()).isEqualTo("author1");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt1);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getPostId()).isEqualTo(postId);
        assertThat(result.get(1).getContent()).isEqualTo("content2");
        assertThat(result.get(1).getAuthor()).isEqualTo("author2");
        assertThat(result.get(1).getCreatedAt()).isEqualTo(createdAt2);
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록을 반환")
    void findByPostIdWhenEmpty() {
        final Long postId = 1L;
        when(commentRepository.findByPostIdOrderByCreatedAtDesc(postId)).thenReturn(List.of());

        final List<CommentResponse> result = commentService.findByPostId(postId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유효한 요청이면 댓글 저장하고 정보 반환")
    void create() {
        final Long postId = 1L;
        final String content = "content";
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var saved = new Comment(1L, postId, content, author, createdAt);
        when(postRepository.existsById(postId)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        final CommentResponse result = commentService.create(postId, content, author);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("게시글이 없으면 댓글 작성 실패")
    void createWhenPostNotFound() {
        final Long postId = 1L;
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThatThrownBy(() -> commentService.create(postId, "content", "author"))
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
        verify(postRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("댓글이 100자를 넘으면 작성 실패")
    void createWhenContentTooLong() {
        final String content = "a".repeat(101);

        assertThatThrownBy(() -> commentService.create(1L, content, "author"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("너무 길어요");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("유효한 요청이면 댓글 수정하고 정보 반환")
    void updateComment() {
        final Long id = 1L;
        final Long postId = 1L;
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var comment = new Comment(id, postId, "content", author, createdAt);
        final var updated = new Comment(id, postId, "newContent", author, createdAt);
        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(updated);

        final CommentResponse result = commentService.update(id, "newContent", author);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getContent()).isEqualTo("newContent");
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 없으면 수정 실패")
    void updateCommentWhenNotFound() {
        final Long id = 1L;
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.update(id, "newContent", "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("댓글 없음");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("작성자가 아니면 수정 실패")
    void updateCommentWhenNotAuthor() {
        final Long id = 1L;
        final var comment = new Comment(id, 1L, "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0));
        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(id, "newContent", "other"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 비어 있으면 수정 실패")
    void updateCommentWhenContentEmpty() {
        assertThatThrownBy(() -> commentService.update(1L, "", "author"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 입력하세요");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(commentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("댓글이 100자를 넘으면 수정 실패")
    void updateCommentWhenContentTooLong() {
        final String content = "a".repeat(101);

        assertThatThrownBy(() -> commentService.update(1L, content, "author"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("너무 길어요");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(commentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("유효한 요청이면 댓글 삭제 성공")
    void deleteComment() {
        final Long id = 1L;
        final String author = "author";
        final var comment = new Comment(id, 1L, "content", author, LocalDateTime.of(2026, 1, 1, 10, 0));
        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));

        commentService.delete(id, author);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("댓글이 없으면 삭제 실패")
    void deleteCommentWhenNotFound() {
        final Long id = 1L;
        when(commentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(id, "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("댓글 없음");

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 실패")
    void deleteCommentWhenNotAuthor() {
        final Long id = 1L;
        final var comment = new Comment(id, 1L, "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0));
        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(id, "other"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(commentRepository, never()).delete(any(Comment.class));
    }
}
