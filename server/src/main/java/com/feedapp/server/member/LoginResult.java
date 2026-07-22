package com.feedapp.server.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스와 컨트롤러 간 dto
 */
@Getter
@RequiredArgsConstructor
public class LoginResult {

    private final Long id;
    private final String username;
    private final String accessToken;
    private final String refreshToken;
}
