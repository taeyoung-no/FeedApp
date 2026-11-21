package com.feedapp.server.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CacheMetricsController {

    private final CacheMetrics cacheMetrics;

    @GetMapping("/metrics/cache")
    @ResponseStatus(HttpStatus.OK)
    public CacheMetricsResponse find() {
        return cacheMetrics.snapshot();
    }

    @PostMapping("/metrics/cache/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        cacheMetrics.reset();
    }
}
