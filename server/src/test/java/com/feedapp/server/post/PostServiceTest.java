package com.feedapp.server.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.feedapp.server.common.ForbiddenException;
import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.storage.ImageService;
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

    @Mock
    ImageService imageService;

    @InjectMocks
    PostService postService;

    @Test
    @DisplayName("게시글이 있으면 생성 시각 내림차순 목록을 반환")
    void findAll() {
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                post(1L, "title1", "content1", "author1", createdAt1, List.of()),
                post(2L, "title2", "content2", "author2", createdAt2, List.of())
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
    @DisplayName("게시글이 있으면 id로 상세 조회")
    void findById() {
        final Long id = 1L;
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postRepository.findById(id)).thenReturn(Optional.of(
                post(id, "title", "content", "author", createdAt, List.of())
        ));

        final PostResponse result = postService.findById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("title");
        assertThat(result.getContent()).isEqualTo("content");
        assertThat(result.getAuthor()).isEqualTo("author");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("게시글이 없으면 상세 조회 실패")
    void findByIdWhenNotFound() {
        final Long id = 1L;
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 저장하고 정보 반환")
    void create() {
        final String title = "title";
        final String content = "content";
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var saved = post(1L, title, content, author, createdAt, List.of());
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        final PostResponse result = postService.create(title, content, author, null);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 삭제 성공")
    void deletePost() {
        final Long id = 1L;
        final String author = "author";
        final var post = post(id, "title", "content", author, LocalDateTime.of(2026, 1, 1, 10, 0), List.of());
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        postService.delete(id, author);

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("게시글 삭제 시 연결된 S3 객체도 삭제")
    void deletePostWithImages() {
        final Long id = 1L;
        final String author = "author";
        final var post = post(
                id,
                "title",
                "content",
                author,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                List.of("posts/a.jpg", "posts/b.png")
        );
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        postService.delete(id, author);

        verify(imageService).delete("posts/a.jpg");
        verify(imageService).delete("posts/b.png");
        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("게시글이 없으면 삭제 실패")
    void deletePostWhenNotFound() {
        final Long id = 1L;
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(id, "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 실패")
    void deletePostWhenNotAuthor() {
        final Long id = 1L;
        final var post = post(id, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), List.of());
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(id, "other"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("유효한 요청이면 게시글 수정하고 정보 반환")
    void updatePost() {
        final Long id = 1L;
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var post = post(id, "title", "content", author, createdAt, List.of());
        final var updated = post(id, "newTitle", "newContent", author, createdAt, List.of());
        when(postRepository.findById(id)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(updated);

        final PostResponse result = postService.update(id, "newTitle", "newContent", author, List.of());

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("newTitle");
        assertThat(result.getContent()).isEqualTo("newContent");
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글이 없으면 수정 실패")
    void updatePostWhenNotFound() {
        final Long id = 1L;
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(id, "newTitle", "newContent", "author", List.of()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("작성자가 아니면 수정 실패")
    void updatePostWhenNotAuthor() {
        final Long id = 1L;
        final var post = post(id, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), List.of());
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(id, "newTitle", "newContent", "other", List.of()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("이미지 포함 유효한 요청이면 수정 성공")
    void updatePostWithImageKeys() {
        final Long id = 1L;
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var existingKeys = List.of("posts/old.jpg");
        final var newKeys = List.of("posts/a.jpg", "posts/b.png");
        final var post = post(id, "title", "content", author, createdAt, existingKeys);
        final var updated = post(id, "newTitle", "newContent", author, createdAt, newKeys);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(updated);
        when(imageService.createDownloadUrl("posts/a.jpg")).thenReturn("https://example.com/a");
        when(imageService.createDownloadUrl("posts/b.png")).thenReturn("https://example.com/b");

        final PostResponse result = postService.update(id, "newTitle", "newContent", author, newKeys);

        assertThat(result.getImages()).containsExactly(
                new PostImageResponse("posts/a.jpg", "https://example.com/a"),
                new PostImageResponse("posts/b.png", "https://example.com/b")
        );
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("이미지 키가 있으면 저장하고 key, url 응답에 포함")
    void createWithImageKeys() {
        final String title = "title";
        final String content = "content";
        final String author = "author";
        final var imageKeys = List.of("posts/a.jpg", "posts/b.png");
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var saved = post(1L, title, content, author, createdAt, imageKeys);
        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(imageService.createDownloadUrl("posts/a.jpg")).thenReturn("https://example.com/a");
        when(imageService.createDownloadUrl("posts/b.png")).thenReturn("https://example.com/b");

        final PostResponse result = postService.create(title, content, author, imageKeys);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getImages()).containsExactly(
                new PostImageResponse("posts/a.jpg", "https://example.com/a"),
                new PostImageResponse("posts/b.png", "https://example.com/b")
        );

        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("이미지 키가 null이면 빈 목록으로 저장")
    void createWithNullImageKeys() {
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var saved = post(1L, "title", "content", "author", createdAt, List.of());
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        final PostResponse result = postService.create("title", "content", "author", null);

        assertThat(result.getImages()).isEmpty();
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("상세 조회 시 이미지 key, url 포함")
    void findByIdWithImages() {
        final Long id = 1L;
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final var imageKeys = List.of("posts/a.jpg");
        when(postRepository.findById(id)).thenReturn(Optional.of(
                post(id, "title", "content", "author", createdAt, imageKeys)
        ));
        when(imageService.createDownloadUrl("posts/a.jpg")).thenReturn("https://example.com/a");

        final PostResponse result = postService.findById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getImages()).containsExactly(
                new PostImageResponse("posts/a.jpg", "https://example.com/a")
        );
    }

    @Test
    @DisplayName("상세 조회 시 이미지가 없으면 빈 목록")
    void findByIdWithoutImages() {
        final Long id = 1L;
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postRepository.findById(id)).thenReturn(Optional.of(
                post(id, "title", "content", "author", createdAt, List.of())
        ));

        final PostResponse result = postService.findById(id);

        assertThat(result.getImages()).isEmpty();
    }

    @Test
    @DisplayName("목록 조회 시 이미지 key, url 포함")
    void findAllWithImages() {
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                post(1L, "title", "content", "author", createdAt, List.of("posts/a.jpg"))
        ));
        when(imageService.createDownloadUrl("posts/a.jpg")).thenReturn("https://example.com/a");

        final List<PostResponse> result = postService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getImages()).containsExactly(
                new PostImageResponse("posts/a.jpg", "https://example.com/a")
        );
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
