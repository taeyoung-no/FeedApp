package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	MemberRepository memberRepository;

	@Spy
	JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
		"12345678901234567890123456789012",
		3_600_000L
	);

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

	@Test
	@DisplayName("username 길이가 0이면 회원가입 실패")
	void signupWithEmptyUsername() {
		assertThatThrownBy(() -> memberService.signup("", "password"))
			.isInstanceOf(IllegalArgumentException.class);

		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("username 길이가 8 초과이면 회원가입 실패")
	void signupWithTooLongUsername() {
		assertThatThrownBy(() -> memberService.signup("long-username", "password"))
			.isInstanceOf(IllegalArgumentException.class);

		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("password 길이가 0이면 회원가입 실패")
	void signupWithEmptyPassword() {
		assertThatThrownBy(() -> memberService.signup("username", ""))
			.isInstanceOf(IllegalArgumentException.class);

		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("password 길이가 8 초과이면 회원가입 실패")
	void signupWithTooLongPassword() {
		assertThatThrownBy(() -> memberService.signup("username", "long-password"))
			.isInstanceOf(IllegalArgumentException.class);

		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("유효한 요청이면 로그인 성공하고 토큰과 회원 정보 반환")
	void login() {
		final String username = "username";
		final String password = "password";
		final var member = new Member(1L, username, password);
		when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));

		final LoginResponse result = memberService.login(username, password);
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUsername()).isEqualTo(username);
		assertThat(result.getToken()).isNotBlank();
	}

	@Test
	@DisplayName("username이 일치하지 않으면 로그인 실패")
	void loginWithWrongUsername() {
		final String username = "username";
		when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> memberService.login(username, "password"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("password가 일치하지 않으면 로그인 실패")
	void loginWithWrongPassword() {
		final String username = "username";
		when(memberRepository.findByUsername(username))
			.thenReturn(Optional.of(new Member(1L, username, "password")));

		assertThatThrownBy(() -> memberService.login(username, "wrong"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("username 길이가 0이면 로그인 실패")
	void loginWithEmptyUsername() {
		assertThatThrownBy(() -> memberService.login("", "password"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("username 길이가 8 초과이면 로그인 실패")
	void loginWithTooLongUsername() {
		assertThatThrownBy(() -> memberService.login("long-username", "password"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("password 길이가 0이면 로그인 실패")
	void loginWithEmptyPassword() {
		assertThatThrownBy(() -> memberService.login("username", ""))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("password 길이가 8 초과이면 로그인 실패")
	void loginWithTooLongPassword() {
		assertThatThrownBy(() -> memberService.login("username", "long-password"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
