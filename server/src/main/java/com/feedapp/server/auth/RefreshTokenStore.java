package com.feedapp.server.auth;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

    private final StringRedisTemplate stringRedisTemplate;
    private final long expirationMs;

    public RefreshTokenStore(
            StringRedisTemplate stringRedisTemplate,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.expirationMs = expirationMs;
    }

    public void save(String sid, String jti) {
        stringRedisTemplate.opsForValue().set(sid, jti, Duration.ofMillis(expirationMs));
    }

    public Optional<String> find(String sid) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(sid));
    }

    public void delete(String sid) {
        stringRedisTemplate.delete(sid);
    }
}
