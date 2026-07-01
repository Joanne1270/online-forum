package com.forum.notification.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationUnreadSummaryVO {

    private long reply;
    private long mention;
    private long like;
    private long system;
    private Map<String, Long> byCategory;
}
