package com.feedapp.server.like;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.member.ConflictException;
import com.feedapp.server.member.Member;
import com.feedapp.server.member.MemberRepository;
import com.feedapp.server.member.UnauthorizedException;
import com.feedapp.server.post.Post;
import com.feedapp.server.post.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    PostLikeRepository postLikeRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    PostLikeService postLikeService;

    @Test
    @DisplayName("유효한 요청이면 좋아요 저장")
    void like() {
        final Long postId = 1L;
        final String username = "author";
        final Post post = post(postId);
        final Member member = member(10L, username);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(postLikeRepository.existsByMemberIdAndPostId(member.getId(), postId)).thenReturn(false);

        postLikeService.like(postId, username);

        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    @DisplayName("게시글이 없으면 좋아요 실패")
    void likeWhenPostNotFound() {
        final Long postId = 1L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.like(postId, "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    @DisplayName("회원이 없으면 좋아요 실패")
    void likeWhenMemberNotFound() {
        final Long postId = 1L;
        when(postRepository.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(memberRepository.findByUsername("author")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.like(postId, "author"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 인증 정보임");

        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    @DisplayName("이미 좋아요 했으면 실패")
    void likeWhenAlreadyLiked() {
        final Long postId = 1L;
        final String username = "author";
        final Member member = member(10L, username);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(postLikeRepository.existsByMemberIdAndPostId(member.getId(), postId)).thenReturn(true);

        assertThatThrownBy(() -> postLikeService.like(postId, username))
                .isInstanceOf(ConflictException.class)
                .hasMessage("이미 좋아요 함");

        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    @DisplayName("unique 위반이면 ConflictException")
    void likeWhenUniqueConstraintViolated() {
        final Long postId = 1L;
        final String username = "author";
        final Member member = member(10L, username);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post(postId)));
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(postLikeRepository.existsByMemberIdAndPostId(member.getId(), postId)).thenReturn(false);
        when(postLikeRepository.save(any(PostLike.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> postLikeService.like(postId, username))
                .isInstanceOf(ConflictException.class)
                .hasMessage("이미 좋아요 함");
    }

    @Test
    @DisplayName("유효한 요청이면 좋아요 삭제")
    void unlike() {
        final Long postId = 1L;
        final String username = "author";
        final Member member = member(10L, username);
        final PostLike like = new PostLike(1L, member, post(postId), LocalDateTime.of(2026, 1, 1, 10, 0));
        when(postRepository.existsById(postId)).thenReturn(true);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(postLikeRepository.findByMemberIdAndPostId(member.getId(), postId)).thenReturn(Optional.of(like));

        postLikeService.unlike(postId, username);

        verify(postLikeRepository).delete(like);
    }

    @Test
    @DisplayName("게시글이 없으면 좋아요 취소 실패")
    void unlikeWhenPostNotFound() {
        final Long postId = 1L;
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThatThrownBy(() -> postLikeService.unlike(postId, "author"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("게시글 없음");

        verify(postLikeRepository, never()).delete(any(PostLike.class));
    }

    @Test
    @DisplayName("좋아요가 없으면 취소 실패")
    void unlikeWhenLikeNotFound() {
        final Long postId = 1L;
        final String username = "author";
        final Member member = member(10L, username);
        when(postRepository.existsById(postId)).thenReturn(true);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        when(postLikeRepository.findByMemberIdAndPostId(member.getId(), postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.unlike(postId, username))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("좋아요 없음");

        verify(postLikeRepository, never()).delete(any(PostLike.class));
    }

    private static Post post(Long id) {
        return new Post(id, "title", "content", "author", LocalDateTime.of(2026, 1, 1, 10, 0), new ArrayList<>());
    }

    private static Member member(Long id, String username) {
        return new Member(id, username, "password");
    }
}
