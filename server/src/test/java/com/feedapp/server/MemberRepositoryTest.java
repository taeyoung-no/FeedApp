package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class MemberRepositoryTest {

	@Autowired
	MemberRepository memberRepository;

	@Test
	@DisplayName("유효한 요청이면 정상 저장")
	void saveAndExistsByUsername() {
		memberRepository.save(new Member(null, "username", "password"));

		assertThat(memberRepository.existsByUsername("username")).isTrue();
		assertThat(memberRepository.existsByUsername("other")).isFalse();
	}
}
