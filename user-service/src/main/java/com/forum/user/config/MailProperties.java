package com.forum.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "forum.mail")
public class MailProperties {

    private String from = "liuli_jin@outlook.com";

    private String fromName = "在线社区论坛";
}
