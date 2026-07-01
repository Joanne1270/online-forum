package com.forum.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostCreateRequest {

    @NotNull(message = "请选择版块")
    private Long boardId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    private Boolean confirmSimilar;
}
