package com.feedapp.server.post;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.feedapp.server.cache.CacheMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PopularPostCache {

    private static final String KEY = "post:popular";

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
        String cached = stringRedisTemplate.opsForValue().get(KEY);
        if (cached != null) {
            cacheMetrics.incrementCacheHit();
            return Arrays.asList(objectMapper.readValue(cached, PopularPost[].class));
        }
        cacheMetrics.incrementDbRead();
        List<PopularPost> loaded = loader.get();
        stringRedisTemplate.opsForValue().set(KEY, objectMapper.writeValueAsString(loaded), Duration.ofMillis(ttlMs));
        return loaded;
    }
}
