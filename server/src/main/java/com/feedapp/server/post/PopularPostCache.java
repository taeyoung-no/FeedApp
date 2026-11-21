package com.feedapp.server.post;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.feedapp.server.cache.CacheMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PopularPostCache {

    private static final String KEY = "post:popular";
    private static final String LOCK_KEY = "post:popular:lock";
    private static final long LOCK_TTL_MS = 3_000;
    private static final long WAIT_BACKOFF_MS = 20;
    private static final int MAX_RETRIES = 50;

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> SINGLEFLIGHT_SCRIPT =
            RedisScript.of(new ClassPathResource("singleflight.lua"), List.class);
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT =
            RedisScript.of(new ClassPathResource("release-lock.lua"), Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheMetrics cacheMetrics;
    private final long ttlMs;

    public PopularPostCache(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            CacheMetrics cacheMetrics,
            @Value("${cache.popular.ttl-ms}") long ttlMs
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheMetrics = cacheMetrics;
        this.ttlMs = ttlMs;
    }

    public List<PopularPost> getOrLoad(Supplier<List<PopularPost>> loader) {
        String token = UUID.randomUUID().toString();
        for (int i = 0; i < MAX_RETRIES; i++) {
            List<?> result = stringRedisTemplate.execute(
                    SINGLEFLIGHT_SCRIPT,
                    List.of(KEY, LOCK_KEY),
                    token,
                    String.valueOf(LOCK_TTL_MS)
            );
            switch ((String) result.get(0)) {
                case "HIT" -> {
                    cacheMetrics.incrementCacheHit();
                    return Arrays.asList(objectMapper.readValue((String) result.get(1), PopularPost[].class));
                }
                case "LOAD" -> {
                    try {
                        cacheMetrics.incrementDbRead();
                        List<PopularPost> loaded = loader.get();
                        stringRedisTemplate.opsForValue()
                                .set(KEY, objectMapper.writeValueAsString(loaded), Duration.ofMillis(ttlMs));
                        return loaded;
                    } finally {
                        stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(LOCK_KEY), token);
                    }
                }
                default -> sleep();
            }
        }
        throw new IllegalStateException("뭔가 잘못됨");
    }

    private void sleep() {
        try {
            Thread.sleep(WAIT_BACKOFF_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
