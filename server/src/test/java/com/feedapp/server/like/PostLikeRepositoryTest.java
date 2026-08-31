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
    @DisplayName("유효한 요청이면 좋아요 정상 저장")
    void save() {
        final Member member = saveMember("author");
        final Post post = savePost();
        final var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        final PostLike saved = postLikeRepository.save(new PostLike(null, member, post, createdAt));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
    }

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
    @DisplayName("게시글별 좋아요 수 조회")
    void countByPostId() {
        final Member member1 = saveMember("a");
        final Member member2 = saveMember("b");
        final Post post = savePost();
        final Post other = savePost();
        postLikeRepository.save(new PostLike(null, member1, post, LocalDateTime.of(2026, 1, 1, 10, 0)));
        postLikeRepository.save(new PostLike(null, member2, post, LocalDateTime.of(2026, 1, 1, 11, 0)));
        postLikeRepository.save(new PostLike(null, member1, other, LocalDateTime.of(2026, 1, 1, 12, 0)));

        assertThat(postLikeRepository.countByPostId(post.getId())).isEqualTo(2L);
        assertThat(postLikeRepository.countByPostId(other.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원과 게시글로 좋아요 존재 여부 조회")
    void existsByMemberIdAndPostId() {
        final Member member = saveMember("author");
        final Post post = savePost();
        postLikeRepository.save(new PostLike(null, member, post, LocalDateTime.of(2026, 1, 1, 10, 0)));

        assertThat(postLikeRepository.existsByMemberIdAndPostId(member.getId(), post.getId())).isTrue();
        assertThat(postLikeRepository.existsByMemberUsernameAndPostId("author", post.getId())).isTrue();
        assertThat(postLikeRepository.existsByMemberUsernameAndPostId("other", post.getId())).isFalse();
    }

    @Test
    @DisplayName("저장된 좋아요 삭제")
    void delete() {
        final Member member = saveMember("author");
        final Post post = savePost();
        final PostLike saved = postLikeRepository.save(
                new PostLike(null, member, post, LocalDateTime.of(2026, 1, 1, 10, 0))
        );

        postLikeRepository.delete(saved);

        assertThat(postLikeRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시글 삭제 시 좋아요도 cascade 삭제")
    void deletePostCascadesLikes() {
        final Member member = saveMember("author");
        final Post post = savePost();
        final Post other = savePost();
        final PostLike deleted = postLikeRepository.save(
                new PostLike(null, member, post, LocalDateTime.of(2026, 1, 1, 10, 0))
        );
        final PostLike kept = postLikeRepository.save(
                new PostLike(null, member, other, LocalDateTime.of(2026, 1, 1, 11, 0))
        );

        postRepository.delete(post);
        entityManager.flush();
        entityManager.clear();

        assertThat(postLikeRepository.findById(deleted.getId())).isEmpty();
        assertThat(postLikeRepository.findById(kept.getId())).isPresent();
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
