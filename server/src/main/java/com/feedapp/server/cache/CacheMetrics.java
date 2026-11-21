package com.feedapp.server.cache;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class CacheMetrics {

    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong dbReads = new AtomicLong();

    public void incrementCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void incrementDbRead() {
        dbReads.incrementAndGet();
    }

    public CacheMetricsResponse snapshot() {
        return new CacheMetricsResponse(cacheHits.get(), dbReads.get());
    }

    public void reset() {
        cacheHits.set(0);
        dbReads.set(0);
    }
}
