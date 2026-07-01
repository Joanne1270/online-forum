package com.forum.notification.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {

    private Long id;
    private Long userId;
    private String type;
    private Long refId;
    private String refType;
    private Long postId;
    private Long fromUserId;
    private String content;
    private Integer readFlag;
    private LocalDateTime createdAt;
}
