package com.feedapp.server;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
}
