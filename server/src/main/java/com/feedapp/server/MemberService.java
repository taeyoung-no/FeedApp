package com.feedapp.server;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberResponse signup(String username, String password) {
		validateLength(username);
		validateLength(password);
		if (memberRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("username already exists");
		}
		Member saved = memberRepository.save(new Member(null, username, password));
		return new MemberResponse(saved.getId(), saved.getUsername());
	}

	private void validateLength(String input) {
		if (input == null || input.length() == 0) {
			throw new IllegalArgumentException("짧아요");
		}
		if (input.length() > 8) {
			throw new IllegalArgumentException("길어요");
		}
	}
}
