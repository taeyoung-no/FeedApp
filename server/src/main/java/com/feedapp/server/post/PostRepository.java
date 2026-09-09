package com.feedapp.server.post;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.images
            WHERE p.id IN :ids
            """)
    List<Post> findAllWithImagesByIdIn(@Param("ids") Collection<Long> ids);

    default Page<Post> findAllWithImages(Pageable pageable) {
        Page<Post> posts = findAllByOrderByCreatedAtDesc(pageable);
        if (posts.isEmpty()) {
            return posts;
        }
        Map<Long, Post> byId = findAllWithImagesByIdIn(
                posts.getContent().stream().map(Post::getId).toList()
        ).stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        return new PageImpl<>(
                posts.getContent().stream()
                        .map((post) -> byId.getOrDefault(post.getId(), post))
                        .toList(),
                posts.getPageable(),
                posts.getTotalElements()
        );
    }

    @Modifying()
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying()
    @Transactional
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id")
    int decrementLikeCount(@Param("id") Long id);
}
