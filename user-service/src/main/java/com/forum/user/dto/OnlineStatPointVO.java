package com.forum.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnlineStatPointVO {

    private String label;
    private LocalDateTime time;
    private Integer count;
}
