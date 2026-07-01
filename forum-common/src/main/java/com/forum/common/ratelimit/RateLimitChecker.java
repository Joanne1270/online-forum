package com.forum.common.ratelimit;

import java.time.Duration;
import java.util.function.Function;

/**
 * Shared fixed-window rate limit helper for Redis {@code INCR} + {@code EXPIRE}.
 */
public final class RateLimitChecker {

    private RateLimitChecker() {
    }

    public interface Counter {
        Long increment(String key);

        void expire(String key, Duration ttl);
    }

    public static boolean allow(Counter counter, String key, int maxRequests, Duration window) {
        Long count = counter.increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            counter.expire(key, window.plusSeconds(1));
        }
        return count <= maxRequests;
    }

    public static boolean allowBurst(Function<String, Boolean> setIfAbsent, String key, Duration window) {
        Boolean allowed = setIfAbsent.apply(key);
        return allowed == null || allowed;
    }
}
