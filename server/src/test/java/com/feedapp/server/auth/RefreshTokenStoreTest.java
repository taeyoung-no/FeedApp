package com.feedapp.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@DataRedisTest
@Import(RefreshTokenStore.class)
@Testcontainers
class RefreshTokenStoreTest {

    private static final long REFRESH_EXPIRATION_MS = 86_400_000L;

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.refresh-expiration-ms", () -> REFRESH_EXPIRATION_MS);
    }

    @Autowired
    RefreshTokenStore refreshTokenStore;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("sid-jti 저장, 조회, 삭제")
    void saveFindAndDelete() {
        final String sid = UUID.randomUUID().toString();
        final String jti = UUID.randomUUID().toString();

        refreshTokenStore.save(sid, jti);

        assertThat(refreshTokenStore.find(sid)).contains(jti);

        refreshTokenStore.delete(sid);

        assertThat(refreshTokenStore.find(sid)).isEmpty();
    }

    @Test
    @DisplayName("jti 갱신")
    void updateJti() {
        final String sid = UUID.randomUUID().toString();
        refreshTokenStore.save(sid, UUID.randomUUID().toString());
        final String newJti = UUID.randomUUID().toString();

        refreshTokenStore.save(sid, newJti);

        assertThat(refreshTokenStore.find(sid)).contains(newJti);
    }

    @Test
    @DisplayName("저장 시 ttl이 refresh 만료 시간과 같음")
    void saveTtlMatchesRefreshExpiration() {
        final String sid = UUID.randomUUID().toString();
        final long expectedTtlSeconds = REFRESH_EXPIRATION_MS / 1000;

        refreshTokenStore.save(sid, UUID.randomUUID().toString());

        final Long ttlSeconds = stringRedisTemplate.getExpire(sid, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isNotNull().isBetween(expectedTtlSeconds - 5, expectedTtlSeconds);
    }
}
