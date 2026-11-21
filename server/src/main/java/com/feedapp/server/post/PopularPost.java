package com.feedapp.server.post;

import java.time.LocalDateTime;
import java.util.List;

public record PopularPost(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        long likeCount,
        List<String> imageKeys
) {

    public static PopularPost from(Post post) {
        List<String> imageKeys = post.getImages().stream()
                .map(PostImage::getImageKey)
                .toList();
        return new PopularPost(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt(),
                post.getLikeCount(),
                imageKeys
        );
    }
}
