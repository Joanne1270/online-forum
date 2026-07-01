package com.forum.user.dto;

import com.forum.user.entity.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileVO extends UserVO {

    private String pendingNickname;
    private String pendingAvatar;
    private Integer nicknameChangesToday;
    private Integer avatarChangesToday;
    private Boolean showPosts;
    private Boolean showFavorites;
    private Boolean showReplies;
    private Boolean showFollowing;
    private Boolean showFollowers;

    public static UserProfileVO from(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setBirthMonth(user.getBirthMonth());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setBannedUntil(user.getBannedUntil());
        vo.setShowPosts(isPublic(user.getPrivacyPosts()));
        vo.setShowFavorites(isPublic(user.getPrivacyFavorites()));
        vo.setShowReplies(isPublic(user.getPrivacyReplies()));
        vo.setShowFollowing(isPublic(user.getPrivacyFollowing()));
        vo.setShowFollowers(isPublic(user.getPrivacyFollowers()));
        return vo;
    }

    private static boolean isPublic(Integer value) {
        return value == null || value == com.forum.common.constant.ForumConstants.PRIVACY_PUBLIC;
    }
}
