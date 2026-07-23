package com.feedapp.server.post;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/api/posts/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PostResponse findById(@PathVariable Long id) {
        return postService.findById(id);
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

    @DeleteMapping("/api/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        postService.delete(id, authentication.getName());
    }

    @PutMapping("/api/posts/{id}")
    @ResponseStatus(HttpStatus.OK)
    public PostResponse update(
            @PathVariable Long id,
            @RequestBody UpdatePostRequest request,
            Authentication authentication
    ) {
        return postService.update(
                id,
                request.title(),
                request.content(),
                authentication.getName()
        );
    }
}

