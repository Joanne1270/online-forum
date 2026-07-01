package com.forum.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OnlineStatsVO {

    private Integer currentOnline;
    private List<OnlineStatPointVO> points;
}
