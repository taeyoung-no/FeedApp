package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class PostRepositoryTest {

	@Autowired
	PostRepository postRepository;

	@Test
	@DisplayName("게시글이 있으면 생성 시각 내림차순 목록을 반환")
	void findAllByOrderByCreatedAtDesc() {
		final LocalDateTime createdAt1 = LocalDateTime.of(2026, 1, 2, 10, 0);
		final LocalDateTime createdAt2 = LocalDateTime.of(2026, 1, 1, 10, 0);
		final Post saved2 = postRepository.save(new Post(null, "title2", "content2", "author2", createdAt2));
		final Post saved1 = postRepository.save(new Post(null, "title1", "content1", "author1", createdAt1));

		final List<Post> result = postRepository.findAllByOrderByCreatedAtDesc();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(saved1.getId());
		assertThat(result.get(0).getTitle()).isEqualTo("title1");
		assertThat(result.get(0).getContent()).isEqualTo("content1");
		assertThat(result.get(0).getAuthor()).isEqualTo("author1");
		assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt1);
		assertThat(result.get(1).getId()).isEqualTo(saved2.getId());
		assertThat(result.get(1).getTitle()).isEqualTo("title2");
		assertThat(result.get(1).getContent()).isEqualTo("content2");
		assertThat(result.get(1).getAuthor()).isEqualTo("author2");
		assertThat(result.get(1).getCreatedAt()).isEqualTo(createdAt2);
	}

	@Test
	@DisplayName("게시글이 없으면 빈 목록을 반환")
	void findAllByOrderByCreatedAtDescWhenEmpty() {
		final List<Post> result = postRepository.findAllByOrderByCreatedAtDesc();

		assertThat(result).isEmpty();
	}
}
