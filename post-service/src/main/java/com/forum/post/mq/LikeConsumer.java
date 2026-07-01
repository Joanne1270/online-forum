package com.forum.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.ForumMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 点赞计数已在 PostService 中同步更新；此消费者保留用于兼容旧消息，不再修改计数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = ForumConstants.MQ_TOPIC, selectorExpression = ForumConstants.TAG_LIKE, consumerGroup = "post-like-consumer")
public class LikeConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(String message) {
        try {
            objectMapper.readValue(message, ForumMessage.class);
        } catch (Exception e) {
            log.error("Like consumer failed", e);
        }
    }
}
