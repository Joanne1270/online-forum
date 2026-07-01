package com.forum.user.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private String gender;
    private String birthMonth;
}
