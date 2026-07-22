package com.feedapp.server.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MemberResponse {

    private final Long id;
    private final String username;
}
