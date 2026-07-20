package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	MemberRepository memberRepository;

	@InjectMocks
	MemberService memberService;

	@Test
	@DisplayName("유효한 요청이면 정상 저장되고 회원 정보 반환")
	void signup() {
		final String username = "username";
		final String password = "password";
		final var saved = new Member(1L, username, password);
		when(memberRepository.save(any(Member.class))).thenReturn(saved);

		final MemberResponse result = memberService.signup(username, password);
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUsername()).isEqualTo(username);

		verify(memberRepository).save(any(Member.class));
	}

	@Test
	@DisplayName("username 중복이면 회원가입 실패")
	void signupWithDuplicateUsername() {
		final String username = "username";
		final String password = "password";
		when(memberRepository.existsByUsername(username)).thenReturn(true);

		assertThatThrownBy(() -> memberService.signup(username, password))
			.isInstanceOf(IllegalArgumentException.class);

		verify(memberRepository, never()).save(any(Member.class));
	}
}
