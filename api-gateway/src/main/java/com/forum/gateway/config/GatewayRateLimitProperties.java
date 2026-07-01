package com.forum.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "forum.gateway.rate-limit")
public class GatewayRateLimitProperties {

    private boolean enabled = true;
    private int requestsPerMinute = 120;
}
