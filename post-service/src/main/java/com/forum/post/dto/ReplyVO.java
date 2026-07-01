package com.forum.post.dto;

import com.forum.post.entity.Reply;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReplyVO {

    private Long id;
    private Long postId;
    private Long parentId;
    private Long authorId;
    private String authorName;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private LocalDateTime createdAt;
    private Boolean liked;
    private Boolean disliked;
    private Boolean pinned;
    private List<Long> mentionedUserIds;
    private List<ReplyVO> children;

    public static ReplyVO from(Reply reply) {
        ReplyVO vo = new ReplyVO();
        vo.setId(reply.getId());
        vo.setPostId(reply.getPostId());
        vo.setParentId(reply.getParentId());
        vo.setAuthorId(reply.getAuthorId());
        vo.setContent(reply.getContent());
        vo.setLikeCount(reply.getLikeCount());
        vo.setDislikeCount(reply.getDislikeCount());
        vo.setCreatedAt(reply.getCreatedAt());
        if (reply.getChildren() != null && !reply.getChildren().isEmpty()) {
            vo.setChildren(reply.getChildren().stream().map(ReplyVO::from).collect(Collectors.toList()));
        }
        return vo;
    }
}
