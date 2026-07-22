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
        final TokenResponse tokens = issueTokens(member.getUsername(), null);
        return new LoginResponse(
                member.getId(),
                member.getUsername(),
                tokens.getAccessToken(),
                tokens.getRefreshToken()
        );
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validate(refreshToken) || !"refresh".equals(jwtTokenProvider.getType(refreshToken))) {
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
        return issueTokens(username, sid);
    }

    private TokenResponse issueTokens(String username, String sid) {
        final String accessToken = jwtTokenProvider.createAccessToken(username);
        final String newRefreshToken = sid == null
                ? jwtTokenProvider.createRefreshToken(username)
                : jwtTokenProvider.createRefreshToken(username, sid);
        refreshTokenStore.save(
                jwtTokenProvider.getSid(newRefreshToken),
                jwtTokenProvider.getJti(newRefreshToken)
        );
        return new TokenResponse(accessToken, newRefreshToken);
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
