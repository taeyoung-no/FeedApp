package com.feedapp.server.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            SELECT p.id FROM Post p
            WHERE p.createdAt < :createdAt
               OR (p.createdAt = :createdAt AND p.id < :id)
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Long> findNextIds(
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("""
            SELECT p.id FROM Post p
            WHERE p.createdAt > :createdAt
               OR (p.createdAt = :createdAt AND p.id > :id)
            ORDER BY p.createdAt ASC, p.id ASC
            """)
    List<Long> findPrevIds(
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.images
            WHERE p.id IN :ids
            """)
    List<Post> findAllWithImagesByIdIn(@Param("ids") Collection<Long> ids);

    default PostWindow findAllWithImages(String cursor, int pageSize) {
        Pageable limit = PageRequest.of(0, pageSize + 1);
        PostListCursor decoded = (cursor == null || cursor.isBlank())
                ? null
                : PostListCursor.decode(cursor);

        List<Long> ids;
        boolean hasNext;
        boolean hasPrevious;
        if (decoded == null) {
            ids = findAllByOrderByCreatedAtDescIdDesc(limit).stream()
                    .map(Post::getId)
                    .toList();
            hasNext = ids.size() > pageSize;
            hasPrevious = false;
        } else if (!decoded.previous()) {
            ids = findNextIds(decoded.createdAt(), decoded.id(), limit);
            hasNext = ids.size() > pageSize;
            hasPrevious = true;
        } else {
            ids = findPrevIds(decoded.createdAt(), decoded.id(), limit);
            hasPrevious = ids.size() > pageSize;
            hasNext = true;
            ids = new ArrayList<>(ids.subList(0, Math.min(pageSize, ids.size())));
            Collections.reverse(ids);
            return fetchWindow(ids, hasNext, hasPrevious);
        }

        ids = new ArrayList<>(ids.subList(0, Math.min(pageSize, ids.size())));
        return fetchWindow(ids, hasNext, hasPrevious);
    }

    private PostWindow fetchWindow(List<Long> ids, boolean hasNext, boolean hasPrevious) {
        if (ids.isEmpty()) {
            return new PostWindow(List.of(), hasNext, hasPrevious);
        }
        Map<Long, Post> byId = findAllWithImagesByIdIn(ids).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        List<Post> content = ids.stream()
                .map(byId::get)
                .toList();
        return new PostWindow(content, hasNext, hasPrevious);
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
