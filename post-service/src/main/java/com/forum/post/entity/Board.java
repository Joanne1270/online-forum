package com.forum.post.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Board {

    private Long id;
    private String name;
    private String description;
    private String boardType;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
