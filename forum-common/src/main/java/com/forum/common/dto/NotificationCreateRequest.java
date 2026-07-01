package com.forum.common.dto;

import lombok.Data;

@Data
public class NotificationCreateRequest {

    private Long userId;
    private String type;
    private Long refId;
    private String refType;
    private Long postId;
    private Long fromUserId;
    private String content;
}
