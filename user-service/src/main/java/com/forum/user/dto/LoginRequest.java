package com.forum.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "必须是11位有效手机号")
    private String phone;

    @NotBlank(message = "不能为空")
    private String password;
}
