package com.feedapp.server.comment;

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
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentResponse> findByPostId(@PathVariable Long postId) {
        return commentService.findByPostId(postId);
    }

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return commentService.create(postId, request.content(), authentication.getName());
    }

    @PutMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CommentResponse update(
            @PathVariable Long id,
            @RequestBody UpdateCommentRequest request,
            Authentication authentication
    ) {
        return commentService.update(id, request.content(), authentication.getName());
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        commentService.delete(id, authentication.getName());
    }
}

