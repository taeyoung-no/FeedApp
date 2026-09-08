package com.feedapp.server.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.feedapp.server.member.Member;
import com.feedapp.server.member.MemberRepository;
import com.feedapp.server.post.Post;
import com.feedapp.server.post.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PostLikeRepositoryTest {

    @Autowired
    PostLikeRepository postLikeRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("같은 회원이 같은 글에 두 번 좋아요하면 실패")
    void uniqueMemberAndPost() {
        final Member member = saveMember("author");
        final Post post = savePost();
        postLikeRepository.save(new PostLike(null, member, post, LocalDateTime.of(2026, 1, 1, 10, 0)));

        assertThatThrownBy(() -> {
            postLikeRepository.save(new PostLike(null, member, post, LocalDateTime.of(2026, 1, 2, 10, 0)));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("게시글 삭제 시 좋아요도 cascade 삭제")
    void deletePostCascadesLikes() {
        final Member member = saveMember("author");
        final Post post1 = savePost();
        final Post post2 = savePost();
        final PostLike like1 = postLikeRepository.save(
                new PostLike(null, member, post1, LocalDateTime.of(2026, 1, 1, 10, 0))
        );
        final PostLike like2 = postLikeRepository.save(
                new PostLike(null, member, post2, LocalDateTime.of(2026, 1, 1, 11, 0))
        );

        postRepository.delete(post1);
        entityManager.flush();
        entityManager.clear();

        assertThat(postLikeRepository.findById(like1.getId())).isEmpty();
        assertThat(postLikeRepository.findById(like2.getId())).isPresent();
    }

    private Member saveMember(String username) {
        return memberRepository.save(new Member(null, username, "password"));
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
