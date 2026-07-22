package com.feedapp.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
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

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("sid-jti 저장, 조회")
    void saveAndFind() {
        final String sid = UUID.randomUUID().toString();
        final String jti = UUID.randomUUID().toString();

        refreshTokenStore.save(sid, jti);

        assertThat(refreshTokenStore.find(sid)).contains(jti);
    }

    @Test
    @DisplayName("jti 갱신")
    void updateJti() {
        final String sid = UUID.randomUUID().toString();
        final String oldJti = UUID.randomUUID().toString();
        final String newJti = UUID.randomUUID().toString();

        refreshTokenStore.save(sid, oldJti);
        refreshTokenStore.save(sid, newJti);

        assertThat(refreshTokenStore.find(sid)).contains(newJti);
    }

    @Test
    @DisplayName("존재하지 않는 sid 조회 결과 empty")
    void findMissing() {
        final String sid = UUID.randomUUID().toString();

        assertThat(refreshTokenStore.find(sid)).isEmpty();
    }
}
