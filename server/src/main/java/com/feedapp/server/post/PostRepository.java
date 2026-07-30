package com.feedapp.server.post;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.images
            ORDER BY p.createdAt DESC
            """)
    List<Post> findAllByOrderByCreatedAtDesc();
}
