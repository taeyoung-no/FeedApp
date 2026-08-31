package com.feedapp.server.like;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/api/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public void like(@PathVariable Long postId, Authentication authentication) {
        postLikeService.like(postId, authentication.getName());
    }

    @DeleteMapping("/api/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable Long postId, Authentication authentication) {
        postLikeService.unlike(postId, authentication.getName());
    }
}
