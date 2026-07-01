package com.forum.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowUserVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String bio;
    private LocalDateTime followedAt;
}
