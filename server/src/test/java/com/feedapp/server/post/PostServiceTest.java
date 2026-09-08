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
import com.feedapp.server.like.PostLikeRepository;
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

    @Mock
    PostLikeRepository postLikeRepository;

    @InjectMocks
    PostService postService;

    @Test
    @DisplayName("상세 조회 시 좋아요 수와 내 좋아요 여부 포함")
    void findByIdWithLikes() {
        final Long id = 1L;
        final String username = "author";
        when(postRepository.findById(id)).thenReturn(Optional.of(
                new Post(id, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), new ArrayList<>(), 3)
        ));
        when(postLikeRepository.existsByMemberUsernameAndPostId(username, id)).thenReturn(true);

        final PostResponse result = postService.findById(id, username);

        assertThat(result.getLiked()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("게시글이 없으면 상세 조회 실패")
    void findByIdWhenNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findById(1L, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");
    }

    @Test
    @DisplayName("이미지 키가 있으면 저장하고 key, url 응답에 포함")
    void createWithImageKeys() {
        final var imageKeys = List.of("posts/a.jpg", "posts/b.png");
        when(postRepository.save(any(Post.class))).thenReturn(
                post(1L, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), imageKeys)
        );
        when(imageService.createDownloadUrl("posts/a.jpg")).thenReturn("https://example.com/a");
        when(imageService.createDownloadUrl("posts/b.png")).thenReturn("https://example.com/b");

        final PostResponse result = postService.create("title", "content", "author", imageKeys);

        assertThat(result.getImages()).containsExactly(
                new PostImageResponse("posts/a.jpg", "https://example.com/a"),
                new PostImageResponse("posts/b.png", "https://example.com/b")
        );
    }

    @Test
    @DisplayName("게시글 삭제 시 연결된 S3 객체도 삭제")
    void deletePostWithImages() {
        final Long id = 1L;
        final String author = "author";
        final var post = post(id, "title", "content", author, LocalDateTime.of(2026, 1, 1, 10, 0), List.of("posts/a.jpg"));
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        postService.delete(id, author);

        verify(imageService).delete("posts/a.jpg");
        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("게시글이 없으면 삭제 실패")
    void deletePostWhenNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(1L, "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("작성자가 아니면 삭제 실패")
    void deletePostWhenNotAuthor() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(
                post(1L, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), List.of())
        ));

        assertThatThrownBy(() -> postService.delete(1L, "other"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("게시글이 없으면 수정 실패")
    void updatePostWhenNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.update(1L, "newTitle", "newContent", "author", List.of()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("작성자가 아니면 수정 실패")
    void updatePostWhenNotAuthor() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(
                post(1L, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), List.of())
        ));

        assertThatThrownBy(() -> postService.update(1L, "newTitle", "newContent", "other", List.of()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("권한 없음");

        verify(postRepository, never()).save(any(Post.class));
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
        for (int i = 0; i < imageKeys.size(); i++) {
            post.getImages().add(new PostImage(null, post, imageKeys.get(i), i));
        }
        return post;
    }
}
