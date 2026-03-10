package com.fixcart.fixcart.service;

import com.fixcart.fixcart.exception.BadRequestException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RequestRateLimitService {

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    public void assertAllowed(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        RateWindow current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plus(window))) {
                return new RateWindow(now, 1);
            }
            return new RateWindow(existing.windowStart, existing.requestCount + 1);
        });
        if (current.requestCount > maxRequests) {
            throw new BadRequestException("Too many requests. Please try again later.");
        }
    }

    private record RateWindow(Instant windowStart, int requestCount) {
    }
}
