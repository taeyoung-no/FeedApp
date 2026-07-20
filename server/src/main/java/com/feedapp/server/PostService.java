package com.feedapp.server;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;

	public List<PostResponse> findAll() {
		return postRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(this::toResponse)
				.toList();
	}

	public PostResponse create(String title, String content, String author) {
		Post saved = postRepository.save(new Post(null, title, content, author, LocalDateTime.now()));
		return toResponse(saved);
	}

	private PostResponse toResponse(Post post) {
		return new PostResponse(
			post.getId(),
			post.getTitle(),
			post.getContent(),
			post.getAuthor(),
			post.getCreatedAt()
		);
	}
}
