package com.feedapp.server.cache;

public record CacheMetricsResponse(long cacheHits, long dbReads) {
}
