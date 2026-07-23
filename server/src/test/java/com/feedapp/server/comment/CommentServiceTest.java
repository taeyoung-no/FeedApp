package com.feedapp.server.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

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
}
