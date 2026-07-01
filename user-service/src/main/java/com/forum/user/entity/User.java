package com.forum.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private String gender;
    private java.time.LocalDate birthMonth;
    private String role;
    private Integer status;
    private Integer privacyPosts;
    private Integer privacyFavorites;
    private Integer privacyReplies;
    private Integer privacyFollowing;
    private Integer privacyFollowers;
    private LocalDateTime bannedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
