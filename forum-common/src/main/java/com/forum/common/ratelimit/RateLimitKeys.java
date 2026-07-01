package com.forum.common.ratelimit;

import com.forum.common.constant.ForumConstants;

public final class RateLimitKeys {

    private RateLimitKeys() {
    }

    public static String loginWindowKey(String phone, long windowSeconds) {
        long bucket = System.currentTimeMillis() / (windowSeconds * 1000L);
        return ForumConstants.REDIS_RATE_LOGIN + phone + ":" + bucket;
    }

    public static String apiWindowKey(String clientKey, long windowSeconds) {
        long bucket = System.currentTimeMillis() / (windowSeconds * 1000L);
        return ForumConstants.REDIS_RATE_API + clientKey + ":" + bucket;
    }
}
