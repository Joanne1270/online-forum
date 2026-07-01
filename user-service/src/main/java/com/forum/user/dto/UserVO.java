package com.forum.user.dto;

import com.forum.user.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private String gender;
    private java.time.LocalDate birthMonth;
    private String role;
    private Integer status;
    private LocalDateTime bannedUntil;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setBirthMonth(user.getBirthMonth());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setBannedUntil(user.getBannedUntil());
        return vo;
    }

    public static UserVO fromPublic(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setBirthMonth(user.getBirthMonth());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setBannedUntil(user.getBannedUntil());
        return vo;
    }
}
