package com.forum.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "必须是11位有效手机号")
    private String phone;

    @NotBlank(message = "不能为空")
    private String code;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 50, message = "长度需在6-50位之间")
    private String newPassword;
}
