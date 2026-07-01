package com.forum.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyCreateRequest {

    @NotBlank(message = "输入内容不能为空")
    private String content;
    private Long parentId;
}
