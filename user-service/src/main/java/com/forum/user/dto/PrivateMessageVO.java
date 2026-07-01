package com.forum.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateMessageVO {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean read;
    private LocalDateTime createdAt;
    private Boolean mine;
}
