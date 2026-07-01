package com.forum.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    private Long peerId;
    private String peerNickname;
    private String peerAvatar;
    private String lastContent;
    private LocalDateTime lastTime;
    private Long unreadCount;
}
