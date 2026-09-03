package com.feedapp.server.post;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.images
            ORDER BY p.createdAt DESC
            """)
    List<Post> findAllByOrderByCreatedAtDesc();

    @Modifying()
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = ?1")
    int incrementLikeCount(Long id);

    @Modifying()
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = ?1")
    int decrementLikeCount(Long id);
}
