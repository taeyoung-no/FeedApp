package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
class PostServiceTest {

    @Mock
    PostRepository postRepository;

    @InjectMocks
    PostService postService;

    @Test
    @DisplayName("게시글이 있으면 생성 시각 내림차순 목록을 반환")
    void findAll() {
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                new Post(1L, "title1", "content1", "author1", createdAt1),
                new Post(2L, "title2", "content2", "author2", createdAt2)
        ));

        final List<PostResponse> result = postService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("title1");
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getAuthor()).isEqualTo("author1");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt1);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getTitle()).isEqualTo("title2");
        assertThat(result.get(1).getContent()).isEqualTo("content2");
        assertThat(result.get(1).getAuthor()).isEqualTo("author2");
        assertThat(result.get(1).getCreatedAt()).isEqualTo(createdAt2);
    }

    @Test
    @DisplayName("게시글이 없으면 빈 목록을 반환")
    void findAllWhenEmpty() {
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        final List<PostResponse> result = postService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 저장하고 정보 반환")
    void create() {
        final String title = "title";
        final String content = "content";
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var saved = new Post(1L, title, content, author, createdAt);
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        final PostResponse result = postService.create(title, content, author);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        verify(postRepository).save(any(Post.class));
    }
}
