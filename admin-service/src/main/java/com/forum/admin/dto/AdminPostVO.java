package com.forum.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminPostVO {

    private Long id;
    private String title;
    private String authorName;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
