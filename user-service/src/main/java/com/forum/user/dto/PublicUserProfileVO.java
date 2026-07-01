package com.forum.user.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PublicUserProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private Boolean postsVisible;
    private Boolean favoritesVisible;
    private Boolean repliesVisible;
    private Boolean followingVisible;
    private Boolean followersVisible;
    private Integer followingCount;
    private Integer followersCount;
    private List<Map<String, Object>> posts;
    private List<Map<String, Object>> favorites;
    private List<Map<String, Object>> replies;
    private List<FollowUserVO> following;
    private List<FollowUserVO> followers;
    private Boolean followed;
    /** 账号是否已注销 */
    private Boolean deleted;
    /** 主页展示：女 20岁 山东青岛 */
    private String profileBrief;
}
