package com.feedapp.server.auth;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate stringRedisTemplate;

    public void save(String sid, String jti) {
        stringRedisTemplate.opsForValue().set(sid, jti);
    }

    public Optional<String> find(String sid) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(sid));
    }

    public void delete(String sid) {
        stringRedisTemplate.delete(sid);
    }
}
