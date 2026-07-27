package com.feedapp.server.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PostRepositoryTest {

    @Autowired
    PostRepository postRepository;

    @Test
    @DisplayName("게시글이 있으면 생성 시각 내림차순 목록을 반환")
    void findAllByOrderByCreatedAtDesc() {
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        final Post saved2 = postRepository.save(post(null, "title2", "content2", "author2", createdAt2, List.of()));
        final Post saved1 = postRepository.save(post(null, "title1", "content1", "author1", createdAt1, List.of()));

        final List<Post> result = postRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(saved1.getId());
        assertThat(result.get(0).getTitle()).isEqualTo("title1");
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getAuthor()).isEqualTo("author1");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt1);
        assertThat(result.get(1).getId()).isEqualTo(saved2.getId());
        assertThat(result.get(1).getTitle()).isEqualTo("title2");
        assertThat(result.get(1).getContent()).isEqualTo("content2");
        assertThat(result.get(1).getAuthor()).isEqualTo("author2");
        assertThat(result.get(1).getCreatedAt()).isEqualTo(createdAt2);
    }

    @Test
    @DisplayName("게시글이 없으면 빈 목록을 반환")
    void findAllByOrderByCreatedAtDescWhenEmpty() {
        final List<Post> result = postRepository.findAllByOrderByCreatedAtDesc();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("id로 게시글 조회")
    void findById() {
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final Post saved = postRepository.save(post(null, "title", "content", "author", createdAt, List.of()));

        final var result = postRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getTitle()).isEqualTo("title");
        assertThat(result.get().getContent()).isEqualTo("content");
        assertThat(result.get().getAuthor()).isEqualTo("author");
        assertThat(result.get().getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("id에 해당하는 게시글이 없으면 비어 있음")
    void findByIdWhenNotFound() {
        assertThat(postRepository.findById(1L)).isEmpty();
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 정상 저장")
    void save() {
        final String title = "title";
        final String content = "content";
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        final Post saved = postRepository.save(post(null, title, content, author, createdAt, List.of()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo(title);
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getAuthor()).isEqualTo(author);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("저장된 게시글 삭제")
    void delete() {
        final Post saved = postRepository.save(
                post(null, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), List.of())
        );

        postRepository.delete(saved);

        assertThat(postRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("저장된 게시글 수정")
    void update() {
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final Post saved = postRepository.save(
                post(null, "title", "content", "author", createdAt, List.of())
        );

        final Post updated = postRepository.save(
                post(saved.getId(), "newTitle", "newContent", "author", createdAt, List.of())
        );

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getTitle()).isEqualTo("newTitle");
        assertThat(updated.getContent()).isEqualTo("newContent");
        assertThat(updated.getAuthor()).isEqualTo("author");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("이미지 키가 있으면 함께 저장·조회")
    void saveWithImageKeys() {
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var imageKeys = List.of("posts/a.jpg", "posts/b.png");

        final Post saved = postRepository.save(
                post(null, "title", "content", "author", createdAt, imageKeys)
        );

        final var result = postRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getImages()).extracting(PostImage::getImageKey).containsExactly("posts/a.jpg", "posts/b.png");
    }

    @Test
    @DisplayName("이미지 키가 없으면 빈 목록으로 조회")
    void saveWithoutImageKeys() {
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        final Post saved = postRepository.save(
                post(null, "title", "content", "author", createdAt, List.of())
        );

        final var result = postRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getImages()).isEmpty();
    }

    private static Post post(
            Long id,
            String title,
            String content,
            String author,
            LocalDateTime createdAt,
            List<String> imageKeys
    ) {
        Post post = new Post(id, title, content, author, createdAt, new ArrayList<>());
        if (imageKeys != null) {
            for (int i = 0; i < imageKeys.size(); i++) {
                post.getImages().add(new PostImage(null, post, imageKeys.get(i), i));
            }
        }
        return post;
    }
}
