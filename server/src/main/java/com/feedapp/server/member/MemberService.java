package com.feedapp.server.member;

import com.feedapp.server.auth.JwtTokenProvider;
import com.feedapp.server.auth.RefreshTokenStore;
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
            throw new ConflictException("username 중복임");
        }
        Member saved = memberRepository.save(new Member(null, username, password));
        return new MemberResponse(saved.getId(), saved.getUsername());
    }

    public LoginResult login(String username, String password) {
        validateLength(username);
        validateLength(password);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("뭔가 잘못 입력함"));
        if (!member.getPassword().equals(password)) {
            throw new UnauthorizedException("뭔가 잘못 입력함");
        }
        final TokenResponse tokens = issueTokens(member.getUsername(), null);
        return new LoginResult(
                member.getId(),
                member.getUsername(),
                tokens.getAccessToken(),
                tokens.getRefreshToken()
        );
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validate(refreshToken) || !jwtTokenProvider.getType(refreshToken).equals("refresh")) {
            throw new UnauthorizedException("유효하지 않은 인증 정보임");
        }
        final String sid = jwtTokenProvider.getSid(refreshToken);
        final String jti = jwtTokenProvider.getJti(refreshToken);
        final String storedJti = refreshTokenStore.find(sid)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 인증 정보임"));
        if (!storedJti.equals(jti)) {
            throw new UnauthorizedException("유효하지 않은 인증 정보임");
        }
        final String username = jwtTokenProvider.getUsername(refreshToken);
        return issueTokens(username, sid);
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validate(refreshToken) || !jwtTokenProvider.getType(refreshToken).equals("refresh")) {
            throw new UnauthorizedException("유효하지 않은 인증 정보임");
        }
        refreshTokenStore.delete(jwtTokenProvider.getSid(refreshToken));
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
