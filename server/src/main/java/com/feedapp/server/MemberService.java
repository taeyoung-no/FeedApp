package com.feedapp.server;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberResponse signup(String username, String password) {
		Member saved = memberRepository.save(new Member(null, username, password));
		return new MemberResponse(saved.getId(), saved.getUsername());
	}
}
