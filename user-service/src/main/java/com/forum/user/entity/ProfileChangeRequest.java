package com.forum.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileChangeRequest {

    private Long id;
    private Long userId;
    private String fieldType;
    private String oldValue;
    private String newValue;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
