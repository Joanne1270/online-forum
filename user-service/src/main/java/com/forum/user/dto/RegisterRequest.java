package com.forum.user.dto;

import com.forum.common.util.PhoneUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "必须是11位有效手机号")
    private String phone;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 50, message = "长度需在6-50位之间")
    private String password;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "不能超过50字")
    private String nickname;

    @NotBlank(message = "不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "不能超过100字")
    private String email;
}
