package com.forum.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.ForumMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(String tag, ForumMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            rocketMQTemplate.syncSend(ForumConstants.MQ_TOPIC + ":" + tag, MessageBuilder.withPayload(payload).build());
        } catch (Exception e) {
            log.error("Failed to send MQ message tag={}", tag, e);
        }
    }
}
