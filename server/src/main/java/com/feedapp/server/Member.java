package com.feedapp.server;

public class Member {

	private final Long id;
	private final String username;
	private final String password;

	public Member(Long id, String username, String password) {
		this.id = id;
		this.username = username;
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
}
