package com.forum.post.dto;

import lombok.Data;

@Data
public class PostDraftRequest {

    private Long boardId;
    private String title;
    private String content;
    private Boolean confirmSimilar;
}
