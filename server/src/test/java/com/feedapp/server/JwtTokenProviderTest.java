package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "12345678901234567890123456789012",
            3_600_000L
    );

    @Test
    @DisplayName("유효한 토큰이면 검증 성공")
    void validate() {
        final String token = jwtTokenProvider.createToken("username");

        assertThat(jwtTokenProvider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("유효한 토큰이면 username 정상 반환")
    void getUsername() {
        final String token = jwtTokenProvider.createToken("username");

        assertThat(jwtTokenProvider.getUsername(token)).isEqualTo("username");
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
                3_600_000L
        );
        final String token = otherProvider.createToken("username");

        assertThat(jwtTokenProvider.validate(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰이면 검증 실패")
    void validateWithExpiredToken() throws Exception {
        final var shortLived = new JwtTokenProvider(
                "12345678901234567890123456789012",
                1L
        );
        final String token = shortLived.createToken("username");
        Thread.sleep(100);

        assertThat(shortLived.validate(token)).isFalse();
    }
}
