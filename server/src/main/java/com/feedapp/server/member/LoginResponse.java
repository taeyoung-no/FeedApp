package com.feedapp.server.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginResponse {

    private final Long id;
    private final String username;
    private final String accessToken;
    private final String refreshToken;
}
