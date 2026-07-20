package com.feedapp.server;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;

	public List<PostResponse> findAll() {
		return postRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(post -> new PostResponse(
					post.getId(),
					post.getTitle(),
					post.getContent(),
					post.getAuthor(),
					post.getCreatedAt()
				))
				.toList();
	}
}
