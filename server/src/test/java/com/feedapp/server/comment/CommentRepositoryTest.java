package com.feedapp.server.comment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;

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
    @DisplayName("게시글 삭제 시 댓글도 cascade 삭제")
    void deletePostCascadesComments() {
        final Post post1 = savePost();
        final Post post2 = savePost();
        final Comment comment1 = commentRepository.save(
                new Comment(null, post1, "content1", "author", LocalDateTime.of(2026, 1, 1, 10, 0))
        );
        final Comment comment2 = commentRepository.save(
                new Comment(null, post2, "content2", "author", LocalDateTime.of(2026, 1, 2, 10, 0))
        );

        postRepository.delete(post1);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(comment1.getId())).isEmpty();
        assertThat(commentRepository.findById(comment2.getId())).isPresent();
    }

    private Post savePost() {
        return postRepository.save(
                new Post(null, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), new ArrayList<>())
        );
    }
}
