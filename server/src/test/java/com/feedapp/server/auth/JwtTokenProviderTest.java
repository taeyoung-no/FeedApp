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
    @DisplayName("유효한 엑세스 토큰이면 검증 성공")
    void validate() {
        final String token = jwtTokenProvider.createAccessToken("username");

        assertThat(jwtTokenProvider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("유효한 엑세스 토큰이면 username 정상 반환")
    void getUsername() {
        final String token = jwtTokenProvider.createAccessToken("username");

        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("username");
    }

    @Test
    @DisplayName("유효한 엑세스 토큰이면 type(access) 정상 반환")
    void getType() {
        final String token = jwtTokenProvider.createAccessToken("username");

        assertThat(jwtTokenProvider.getType(token)).isEqualTo("access");
    }

    @Test
    @DisplayName("유효한 엑세스 토큰이면 uuid 형식인 jti 정상 반환")
    void getJti() {
        final String token = jwtTokenProvider.createAccessToken("username");

        final String jti = jwtTokenProvider.getJti(token);

        assertThat(jti).isNotBlank();
        assertThatCode(() -> UUID.fromString(jti)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("엑세스 토큰 발급할 때마다 jti 다름")
    void jtiIsUnique() {
        final String token1 = jwtTokenProvider.createAccessToken("username");
        final String token2 = jwtTokenProvider.createAccessToken("username");

        assertThat(jwtTokenProvider.getJti(token1))
                .isNotEqualTo(jwtTokenProvider.getJti(token2));
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 type(refresh) 정상 반환")
    void getRefreshTokenType() {
        final String token = jwtTokenProvider.createRefreshToken("username");

        assertThat(jwtTokenProvider.getType(token)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 username 정상 반환")
    void getRefreshTokenUserType() {
        final String token = jwtTokenProvider.createRefreshToken("username");

        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("username");
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 uuid 형식인 jti 정상 반환")
    void createRefreshToken_hasJtiAsUuid() {
        final String token = jwtTokenProvider.createRefreshToken("username");

        final String jti = jwtTokenProvider.getJti(token);

        assertThat(jti).isNotBlank();
        assertThatCode(() -> UUID.fromString(jti)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 uuid 형식인 sid 정상 반환")
    void createRefreshToken_hasSidAsUuid() {
        final String token = jwtTokenProvider.createRefreshToken("username");

        final String sid = jwtTokenProvider.getSid(token);

        assertThat(sid).isNotBlank();
        assertThatCode(() -> UUID.fromString(sid)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("리프레시 토큰 발급할 때마다 jti 다름")
    void createRefreshToken_jtiIsUnique() {
        final String token1 = jwtTokenProvider.createRefreshToken("username");
        final String token2 = jwtTokenProvider.createRefreshToken("username");

        assertThat(jwtTokenProvider.getJti(token1))
                .isNotEqualTo(jwtTokenProvider.getJti(token2));
    }

    @Test
    @DisplayName("리프레시 토큰 발급할 때마다 sid 다름")
    void createRefreshToken_sidIsUnique() {
        final String token1 = jwtTokenProvider.createRefreshToken("username");
        final String token2 = jwtTokenProvider.createRefreshToken("username");

        assertThat(jwtTokenProvider.getSid(token1))
                .isNotEqualTo(jwtTokenProvider.getSid(token2));
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
        final String token = otherProvider.createAccessToken("username");

        assertThat(jwtTokenProvider.validate(token)).isFalse();
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
