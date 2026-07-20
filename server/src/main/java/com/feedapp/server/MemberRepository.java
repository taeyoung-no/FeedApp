package com.feedapp.server;

public interface MemberRepository {

	Member save(Member member);

	boolean existsByUsername(String username);
}
