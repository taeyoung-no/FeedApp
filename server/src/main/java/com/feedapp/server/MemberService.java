package com.feedapp.server;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberResponse signup(String username, String password) {
		validateLength(username);
		validateLength(password);
		if (memberRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("username 중복임");
		}
		Member saved = memberRepository.save(new Member(null, username, password));
		return new MemberResponse(saved.getId(), saved.getUsername());
	}

	public MemberResponse login(String username, String password) {
		validateLength(username);
		validateLength(password);
		Member member = memberRepository.findByUsername(username)
			.orElseThrow(() -> new IllegalArgumentException("뭔가 잘못 입력함"));
		if (!member.getPassword().equals(password)) {
			throw new IllegalArgumentException("뭔가 잘못 입력함");
		}
		return new MemberResponse(member.getId(), member.getUsername());
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
