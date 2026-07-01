package com.forum.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportCreateRequest {

    @NotBlank(message = "请填写举报原因")
    @Size(max = 500, message = "举报原因不能超过500字")
    private String reason;
}
