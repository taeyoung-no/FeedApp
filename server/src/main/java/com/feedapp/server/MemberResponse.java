package com.feedapp.server;

public class MemberResponse {

	private final Long id;
	private final String username;

	public MemberResponse(Long id, String username) {
		this.id = id;
		this.username = username;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}
}
