package com.forum.user.dto;

import lombok.Data;

@Data
public class PrivacyUpdateRequest {

    private Boolean showPosts;
    private Boolean showFavorites;
    private Boolean showReplies;
    private Boolean showFollowing;
    private Boolean showFollowers;
}
