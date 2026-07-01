package com.forum.post.dto;

import com.forum.post.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PostVO {

    private Long id;
    private Long boardId;
    private String boardName;
    private Long authorId;
    private String authorName;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean draft;
    private Boolean liked;
    private Boolean favorited;
    private Boolean official;
    private Boolean featured;
    private Boolean officialPinned;
    private Boolean profilePinned;
    private Long pinnedReplyId;
    private List<String> tags = new ArrayList<>();
    private List<Long> mentionedUserIds = new ArrayList<>();

    public static PostVO from(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setBoardId(post.getBoardId());
        vo.setAuthorId(post.getAuthorId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setReplyCount(post.getReplyCount());
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());
        vo.setDraft(post.getStatus() != null && post.getStatus() == 2);
        vo.setFeatured(post.getFeatured() != null && post.getFeatured() == 1);
        vo.setOfficialPinned(post.getOfficialPinned() != null && post.getOfficialPinned() == 1);
        vo.setPinnedReplyId(post.getPinnedReplyId());
        return vo;
    }
}
