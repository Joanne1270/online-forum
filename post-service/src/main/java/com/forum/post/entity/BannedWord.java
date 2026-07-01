package com.forum.post.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannedWord {

    private Long id;
    private String word;
    private Integer enabled;
    private LocalDateTime createdAt;
}
