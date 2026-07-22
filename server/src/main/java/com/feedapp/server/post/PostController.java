package com.feedapp.server.post;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/api/posts")
    @ResponseStatus(HttpStatus.OK)
    public List<PostResponse> findAll() {
        return postService.findAll();
    }

    @PostMapping("/api/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @RequestBody CreatePostRequest request,
            Authentication authentication
    ) {
        return postService.create(
                request.title(),
                request.content(),
                authentication.getName()
        );
    }
}

