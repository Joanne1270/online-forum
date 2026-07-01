package com.forum.notification.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.ForumMessage;
import com.forum.notification.entity.Notification;
import com.forum.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = ForumConstants.MQ_TOPIC, selectorExpression = "*", consumerGroup = "notification-consumer")
public class NotificationConsumer implements RocketMQListener<String> {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(String message) {
        try {
            ForumMessage msg = objectMapper.readValue(message, ForumMessage.class);
            if (msg.getToUserId() == null) {
                return;
            }
            if (ForumConstants.TAG_LIKE.equals(msg.getType())
                    && msg.getFromUserId() != null
                    && msg.getFromUserId().equals(msg.getToUserId())) {
                return;
            }
            if (ForumConstants.TAG_FAVORITE.equals(msg.getType())
                    && msg.getFromUserId() != null
                    && msg.getFromUserId().equals(msg.getToUserId())) {
                return;
            }
            Notification notification = new Notification();
            notification.setUserId(msg.getToUserId());
            notification.setType(msg.getType());
            notification.setRefId(msg.getRefId());
            notification.setRefType(msg.getRefType());
            notification.setPostId(resolvePostId(msg));
            notification.setFromUserId(msg.getFromUserId());
            notification.setContent(msg.getContent());
            notificationService.save(notification);
        } catch (Exception e) {
            log.error("Notification consumer failed", e);
        }
    }

    private Long resolvePostId(ForumMessage msg) {
        if (msg.getPostId() != null) {
            return msg.getPostId();
        }
        if ("POST".equals(msg.getRefType())) {
            return msg.getRefId();
        }
        return null;
    }
}
