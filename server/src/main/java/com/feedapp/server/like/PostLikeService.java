package com.feedapp.server.like;

import java.time.LocalDateTime;

import com.feedapp.server.common.NotFoundException;
import com.feedapp.server.member.ConflictException;
import com.feedapp.server.member.Member;
import com.feedapp.server.member.MemberRepository;
import com.feedapp.server.member.UnauthorizedException;
import com.feedapp.server.post.Post;
import com.feedapp.server.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public void like(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        Member member = findMember(username);
        if (postLikeRepository.existsByMemberIdAndPostId(member.getId(), postId)) {
            throw new ConflictException("이미 좋아요 함");
        }
        try {
            postLikeRepository.save(new PostLike(null, member, post, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 좋아요 함");
        }
        postRepository.incrementLikeCount(postId);
    }

    public void unlike(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글 없음"));
        Member member = findMember(username);
        PostLike like = postLikeRepository.findByMemberIdAndPostId(member.getId(), postId)
                .orElseThrow(() -> new NotFoundException("좋아요 없음"));
        postLikeRepository.delete(like);
        postRepository.decrementLikeCount(postId);
    }

    private Member findMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 인증 정보임"));
    }
}
