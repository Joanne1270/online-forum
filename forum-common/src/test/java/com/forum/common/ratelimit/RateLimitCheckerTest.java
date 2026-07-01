package com.forum.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitCheckerTest {

    @Test
    void allowRespectsMaxRequests() {
        Map<String, Long> counts = new HashMap<>();
        RateLimitChecker.Counter counter = new RateLimitChecker.Counter() {
            @Override
            public Long increment(String key) {
                return counts.merge(key, 1L, Long::sum);
            }

            @Override
            public void expire(String key, Duration ttl) {
            }
        };

        assertTrue(RateLimitChecker.allow(counter, "login:138", 3, Duration.ofMinutes(1)));
        assertTrue(RateLimitChecker.allow(counter, "login:138", 3, Duration.ofMinutes(1)));
        assertTrue(RateLimitChecker.allow(counter, "login:138", 3, Duration.ofMinutes(1)));
        assertFalse(RateLimitChecker.allow(counter, "login:138", 3, Duration.ofMinutes(1)));
    }

    @Test
    void allowBurstUsesSetIfAbsent() {
        Map<String, Boolean> keys = new HashMap<>();
        assertTrue(RateLimitChecker.allowBurst(key -> keys.putIfAbsent(key, true) == null, "rate:1", Duration.ofSeconds(3)));
        assertFalse(RateLimitChecker.allowBurst(key -> keys.putIfAbsent(key, true) == null, "rate:1", Duration.ofSeconds(3)));
    }
}
