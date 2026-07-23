package com.feedapp.server.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    CommentRepository commentRepository;

    @Test
    @DisplayName("댓글이 있으면 생성 시각 내림차순 목록을 반환")
    void findByPostIdOrderByCreatedAtDesc() {
        final Long postId = 1L;
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        final Comment saved1 = commentRepository.save(
                new Comment(null, postId, "content1", "author1", createdAt1)
        );
        final Comment saved2 = commentRepository.save(
                new Comment(null, postId, "content2", "author2", createdAt2)
        );
        commentRepository.save(
                new Comment(null, 2L, "other", "author", LocalDateTime.of(2026, 1, 3, 10, 0))
        );

        final List<Comment> result = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(saved1.getId());
        assertThat(result.get(0).getPostId()).isEqualTo(postId);
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getAuthor()).isEqualTo("author1");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt1);
        assertThat(result.get(1).getId()).isEqualTo(saved2.getId());
        assertThat(result.get(1).getPostId()).isEqualTo(postId);
        assertThat(result.get(1).getContent()).isEqualTo("content2");
        assertThat(result.get(1).getAuthor()).isEqualTo("author2");
        assertThat(result.get(1).getCreatedAt()).isEqualTo(createdAt2);
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록을 반환")
    void findByPostIdOrderByCreatedAtDescWhenEmpty() {
        final List<Comment> result = commentRepository.findByPostIdOrderByCreatedAtDesc(1L);

        assertThat(result).isEmpty();
    }
}
