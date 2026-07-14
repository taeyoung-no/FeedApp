package com.feedapp.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Member {

	private final Long id;
	private final String username;
	private final String password;
}
