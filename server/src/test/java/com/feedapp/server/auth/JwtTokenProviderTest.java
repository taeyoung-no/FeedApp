package com.feedapp.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "12345678901234567890123456789012",
            1_800_000L,
            86_400_000L
    );

    @Test
    @DisplayName("액세스 토큰에 username, type, jti 포함")
    void accessTokenContainsClaims() {
        final String token = jwtTokenProvider.createAccessToken("username");

        assertThat(jwtTokenProvider.validate(token)).isTrue();
        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("username");
        assertThat(jwtTokenProvider.getType(token)).isEqualTo("access");
        assertThatCode(() -> UUID.fromString(jwtTokenProvider.getJti(token))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("리프레시 토큰에 username, type, jti, sid 포함")
    void refreshTokenContainsClaims() {
        final String token = jwtTokenProvider.createRefreshToken("username");

        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("username");
        assertThat(jwtTokenProvider.getType(token)).isEqualTo("refresh");
        assertThatCode(() -> UUID.fromString(jwtTokenProvider.getJti(token))).doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(jwtTokenProvider.getSid(token))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("발급할 때마다 jti, sid가 다름")
    void issuedTokensHaveUniqueIds() {
        final String access1 = jwtTokenProvider.createAccessToken("username");
        final String access2 = jwtTokenProvider.createAccessToken("username");
        final String refresh1 = jwtTokenProvider.createRefreshToken("username");
        final String refresh2 = jwtTokenProvider.createRefreshToken("username");

        assertThat(jwtTokenProvider.getJti(access1)).isNotEqualTo(jwtTokenProvider.getJti(access2));
        assertThat(jwtTokenProvider.getJti(refresh1)).isNotEqualTo(jwtTokenProvider.getJti(refresh2));
        assertThat(jwtTokenProvider.getSid(refresh1)).isNotEqualTo(jwtTokenProvider.getSid(refresh2));
    }

    @Test
    @DisplayName("잘못된 토큰이면 검증 실패")
    void validateWithInvalidToken() {
        assertThat(jwtTokenProvider.validate("invalid-token")).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰이면 검증 실패")
    void validateWithWrongSignature() {
        final var otherProvider = new JwtTokenProvider(
                "09876543210987654321098765432109",
                1_800_000L,
                86_400_000L
        );

        assertThat(jwtTokenProvider.validate(otherProvider.createAccessToken("username"))).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰이면 검증 실패")
    void validateWithExpiredToken() throws Exception {
        final var shortLived = new JwtTokenProvider(
                "12345678901234567890123456789012",
                1L,
                1L
        );
        final String token = shortLived.createAccessToken("username");
        Thread.sleep(100);

        assertThat(shortLived.validate(token)).isFalse();
    }
}
