package com.feedapp.server.post;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.feedapp.server.cache.CacheMetrics;
import jakarta.annotation.PreDestroy;
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
    private static final RedisScript<List> SWR_SCRIPT =
            RedisScript.of(new ClassPathResource("swr.lua"), List.class);
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT =
            RedisScript.of(new ClassPathResource("release-lock.lua"), Long.class);

    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheMetrics cacheMetrics;
    private final long ttlMs;
    private final long freshMs;

    public PopularPostCache(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            CacheMetrics cacheMetrics,
            @Value("${cache.popular.ttl-ms}") long ttlMs,
            @Value("${cache.popular.fresh-ms}") long freshMs
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheMetrics = cacheMetrics;
        this.ttlMs = ttlMs;
        this.freshMs = freshMs;
    }

    public List<PopularPost> getOrLoad(Supplier<List<PopularPost>> loader) {
        String token = UUID.randomUUID().toString();
        for (int i = 0; i < MAX_RETRIES; i++) {
            List<?> result = stringRedisTemplate.execute(
                    SWR_SCRIPT,
                    List.of(KEY, LOCK_KEY),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(freshMs),
                    token,
                    String.valueOf(LOCK_TTL_MS)
            );
            switch ((String) result.get(0)) {
                case "HIT" -> {
                    cacheMetrics.incrementCacheHit();
                    return fromJson((String) result.get(1));
                }
                case "STALE_REFRESH" -> {
                    cacheMetrics.incrementCacheHit();
                    refreshExecutor.execute(() -> {
                        try {
                            fill(token, loader);
                        } catch (Exception e) {
                        }
                    });
                    return fromJson((String) result.get(1));
                }
                case "LOAD" -> {
                    return fill(token, loader);
                }
                default -> sleep();
            }
        }
        throw new IllegalStateException("뭔가 잘못됨");
    }

    @PreDestroy
    public void shutdown() {
        refreshExecutor.shutdown();
    }

    private List<PopularPost> fill(String token, Supplier<List<PopularPost>> loader) {
        try {
            cacheMetrics.incrementDbRead();
            List<PopularPost> loaded = loader.get();
            stringRedisTemplate.opsForHash().putAll(KEY, Map.of(
                    "value", objectMapper.writeValueAsString(loaded),
                    "fetchedAtMs", String.valueOf(System.currentTimeMillis())
            ));
            stringRedisTemplate.expire(KEY, Duration.ofMillis(ttlMs));
            return loaded;
        } finally {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(LOCK_KEY), token);
        }
    }

    private List<PopularPost> fromJson(String json) {
        return Arrays.asList(objectMapper.readValue(json, PopularPost[].class));
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
