package com.feedapp.server.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.feedapp.server.post.Post;
import com.feedapp.server.post.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("댓글이 있으면 생성 시각 내림차순 목록을 반환")
    void findByPostIdOrderByCreatedAtDesc() {
        final Post post = savePost();
        final Post otherPost = savePost();
        final var createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
        final var createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
        final Comment saved1 = commentRepository.save(
                new Comment(null, post, "content1", "author1", createdAt1)
        );
        final Comment saved2 = commentRepository.save(
                new Comment(null, post, "content2", "author2", createdAt2)
        );
        commentRepository.save(
                new Comment(null, otherPost, "other", "author", LocalDateTime.of(2026, 1, 3, 10, 0))
        );

        final List<Comment> result = commentRepository.findByPostIdOrderByCreatedAtDesc(post.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(saved1.getId());
        assertThat(result.get(0).getPost().getId()).isEqualTo(post.getId());
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getAuthor()).isEqualTo("author1");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt1);
        assertThat(result.get(1).getId()).isEqualTo(saved2.getId());
        assertThat(result.get(1).getPost().getId()).isEqualTo(post.getId());
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

    @Test
    @DisplayName("유효한 요청이면 댓글 정상 저장")
    void save() {
        final Post post = savePost();
        final String content = "content";
        final String author = "author";
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        final Comment saved = commentRepository.save(new Comment(null, post, content, author, createdAt));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getAuthor()).isEqualTo(author);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("저장된 댓글 삭제")
    void delete() {
        final Post post = savePost();
        final Comment saved = commentRepository.save(
                new Comment(null, post, "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0))
        );

        commentRepository.delete(saved);

        assertThat(commentRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 시 댓글도 cascade 삭제")
    void deletePostCascadesComments() {
        final Post post = savePost();
        final Post otherPost = savePost();
        final Comment deleted1 = commentRepository.save(
                new Comment(null, post, "content1", "author1", LocalDateTime.of(2026, 1, 1, 10, 0))
        );
        final Comment deleted2 = commentRepository.save(
                new Comment(null, post, "content2", "author2", LocalDateTime.of(2026, 1, 1, 11, 0))
        );
        final Comment kept = commentRepository.save(
                new Comment(null, otherPost, "other", "author", LocalDateTime.of(2026, 1, 2, 10, 0))
        );

        postRepository.delete(post);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(deleted1.getId())).isEmpty();
        assertThat(commentRepository.findById(deleted2.getId())).isEmpty();
        assertThat(commentRepository.findById(kept.getId())).isPresent();
    }

    private Post savePost() {
        return postRepository.save(
                new Post(
                        null,
                        "title",
                        "content",
                        "author",
                        LocalDateTime.of(2026, 1, 1, 10, 0),
                        new ArrayList<>()
                )
        );
    }
}
