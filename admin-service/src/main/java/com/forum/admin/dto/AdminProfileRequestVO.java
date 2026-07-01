package com.forum.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminProfileRequestVO {

    private Long id;
    private Long userId;
    private String phone;
    private String nickname;
    private String fieldType;
    private String oldValue;
    private String newValue;
    private Integer status;
    private LocalDateTime createdAt;
}
