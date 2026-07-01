package com.forum.post.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {

    private Long id;
    private Long postId;
    private String postTitle;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private Integer status;
    private LocalDateTime createdAt;
}
