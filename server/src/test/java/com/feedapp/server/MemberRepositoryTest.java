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

	@Test
	@DisplayName("유효한 요청이면 정상 조회")
	void findByUsername() {
		memberRepository.save(new Member(null, "username", "password"));

		final var found = memberRepository.findByUsername("username");

		assertThat(found).isPresent();
		assertThat(found.get().getUsername()).isEqualTo("username");
		assertThat(found.get().getPassword()).isEqualTo("password");
	}

	@Test
	@DisplayName("존재하지 않는 username이면 조회 결과 emtpy")
	void findByUsernameWhenNotExists() {
		assertThat(memberRepository.findByUsername("username")).isEmpty();
	}
}
