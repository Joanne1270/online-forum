package com.forum.post.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Reply {

    private Long id;
    private Long postId;
    private Long parentId;
    private Long authorId;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer status;
    private LocalDateTime createdAt;
    private List<Reply> children = new ArrayList<>();
}
