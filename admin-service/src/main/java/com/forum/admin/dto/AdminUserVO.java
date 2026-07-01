package com.forum.admin.dto;

import lombok.Data;

@Data
public class AdminUserVO {

    private Long id;
    private String phone;
    private String nickname;
    private String role;
    private Integer status;
}
