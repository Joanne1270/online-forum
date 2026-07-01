package com.forum.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.forum.common.constant.ForumConstants;
import com.forum.user.config.UserRedisObjectMapper;
import com.forum.user.entity.User;
import com.forum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final UserRedisObjectMapper userRedisObjectMapper;

    public User getById(Long id) {
        if (id == null) {
            return null;
        }
        String key = ForumConstants.REDIS_CACHE_USER + id;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return userRedisObjectMapper.get().readValue(json, User.class);
            } catch (Exception ignored) {
                redisTemplate.delete(key);
            }
        }
        User user = userMapper.findById(id);
        if (user != null) {
            cacheUser(user);
        }
        return user;
    }

    public void invalidate(Long id) {
        if (id != null) {
            redisTemplate.delete(ForumConstants.REDIS_CACHE_USER + id);
        }
    }

    private void cacheUser(User user) {
        try {
            String json = userRedisObjectMapper.get().writeValueAsString(user);
            redisTemplate.opsForValue().set(
                    ForumConstants.REDIS_CACHE_USER + user.getId(),
                    json,
                    USER_CACHE_TTL
            );
        } catch (JsonProcessingException ignored) {
        }
    }
}
