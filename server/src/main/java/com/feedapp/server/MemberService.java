package com.feedapp.server;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public MemberResponse signup(String username, String password) {
        validateLength(username);
        validateLength(password);
        if (memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username 중복임");
        }
        Member saved = memberRepository.save(new Member(null, username, password));
        return new MemberResponse(saved.getId(), saved.getUsername());
    }

    public LoginResponse login(String username, String password) {
        validateLength(username);
        validateLength(password);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("뭔가 잘못 입력함"));
        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("뭔가 잘못 입력함");
        }
        return issueTokens(member);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validate(refreshToken) || !jwtTokenProvider.getType(refreshToken).equals("refresh")) {
            throw new IllegalArgumentException("뭔가 잘못 입력함");
        }
        final String sid = jwtTokenProvider.getSid(refreshToken);
        final String jti = jwtTokenProvider.getJti(refreshToken);
        final String storedJti = refreshTokenStore.find(sid)
                .orElseThrow(() -> new IllegalArgumentException("뭔가 잘못 입력함"));
        if (!storedJti.equals(jti)) {
            throw new IllegalArgumentException("뭔가 잘못 입력함");
        }
        final String username = jwtTokenProvider.getUsername(refreshToken);
        final Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("뭔가 잘못 입력함"));
        return issueTokens(member, sid);
    }

    private LoginResponse issueTokens(Member member) {
        return issueTokens(member, null);
    }

    private LoginResponse issueTokens(Member member, String sid) {
        final String accessToken = jwtTokenProvider.createAccessToken(member.getUsername());
        final String refreshToken = sid == null
                ? jwtTokenProvider.createRefreshToken(member.getUsername())
                : jwtTokenProvider.createRefreshToken(member.getUsername(), sid);
        refreshTokenStore.save(
                jwtTokenProvider.getSid(refreshToken),
                jwtTokenProvider.getJti(refreshToken)
        );
        return new LoginResponse(
                member.getId(),
                member.getUsername(),
                accessToken,
                refreshToken
        );
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
