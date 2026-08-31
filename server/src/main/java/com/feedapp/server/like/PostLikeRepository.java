package com.feedapp.server.like;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    boolean existsByMemberUsernameAndPostId(String username, Long postId);

    Optional<PostLike> findByMemberIdAndPostId(Long memberId, Long postId);

    long countByPostId(Long postId);
}
