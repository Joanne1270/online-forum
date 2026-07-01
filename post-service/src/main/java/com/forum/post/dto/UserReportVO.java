package com.forum.post.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReportVO {

    private Long id;
    private String refType;
    private Long postId;
    private String title;
    private String reason;
    private Integer status;
    private LocalDateTime createdAt;
}
