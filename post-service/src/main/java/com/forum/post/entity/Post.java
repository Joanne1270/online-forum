package com.forum.post.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Post {

    private Long id;
    private Long boardId;
    private Long authorId;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private Integer status;
    private Integer featured;
    private LocalDateTime featuredAt;
    private Integer officialPinned;
    private LocalDateTime officialPinnedAt;
    private Long pinnedReplyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
