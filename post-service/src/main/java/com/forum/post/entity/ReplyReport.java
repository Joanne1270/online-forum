package com.forum.post.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReplyReport {

    private Long id;
    private Long replyId;
    private Long reporterId;
    private String reason;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
