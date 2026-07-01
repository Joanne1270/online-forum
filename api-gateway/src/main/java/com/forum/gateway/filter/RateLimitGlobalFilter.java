package com.forum.gateway.filter;

import com.forum.common.constant.ForumConstants;
import com.forum.common.ratelimit.RateLimitKeys;
import com.forum.gateway.config.GatewayRateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayRateLimitProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/") || path.startsWith("/api/files/uploads/")) {
            return chain.filter(exchange);
        }

        String clientKey = resolveClientKey(exchange);
        String key = RateLimitKeys.apiWindowKey(clientKey, 60);
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count != null && count == 1L) {
                        return redisTemplate.expire(key, Duration.ofSeconds(61)).thenReturn(count);
                    }
                    return Mono.justOrEmpty(count);
                })
                .defaultIfEmpty(0L)
                .flatMap(count -> {
                    if (count > properties.getRequestsPerMinute()) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(ForumConstants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            return "u:" + userId;
        }
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        var remote = exchange.getRequest().getRemoteAddress();
        String ip = remote != null && remote.getAddress() != null
                ? remote.getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
