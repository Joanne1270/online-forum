package com.forum.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnlineUserSnapshot {

    private Long id;
    private String granularity;
    private LocalDateTime snapshotAt;
    private Integer onlineCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
