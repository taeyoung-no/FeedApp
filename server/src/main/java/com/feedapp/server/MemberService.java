package com.feedapp.server;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberResponse signup(String username, String password) {
		return null;
	}
}
