package com.forum.common.constant;

public final class ForumConstants {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_NICKNAME = "X-Nickname";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    public static final String MQ_TOPIC = "forum-topic";
    public static final String TAG_POST_CREATED = "post-created";
    public static final String TAG_REPLY_CREATED = "reply-created";
    public static final String TAG_MENTION = "mention";
    public static final String TAG_LIKE = "like";
    public static final String TAG_FAVORITE = "favorite";
    public static final String TAG_POST_DELETED = "post-deleted";
    public static final String TAG_REPLY_MODERATED = "reply-moderated";
    public static final String TAG_REPORT_RESULT = "report-result";
    public static final String TAG_PROFILE_APPROVED = "profile-approved";
    public static final String TAG_PROFILE_REJECTED = "profile-rejected";

    public static final int CODE_SENSITIVE_CONTENT = 4221;
    public static final String MSG_SENSITIVE_CONTENT = "您发布的内容包含敏感词汇，请遵守社区规范！";

    public static final int PROFILE_REQUEST_PENDING = 0;
    public static final int PROFILE_REQUEST_APPROVED = 1;
    public static final int PROFILE_REQUEST_REJECTED = 2;
    public static final String PROFILE_FIELD_NICKNAME = "NICKNAME";
    public static final String PROFILE_FIELD_AVATAR = "AVATAR";
    public static final int PROFILE_DAILY_LIMIT = 3;

    public static final String OFFICIAL_AUTHOR_NAME = "社区官方";

    public static final int PRIVACY_PUBLIC = 1;
    public static final int PRIVACY_HIDDEN = 0;

    public static final String REDIS_HOT_POSTS = "hot:posts:";
    public static final String REDIS_POST_DETAIL = "post:detail:v2:";
    public static final String REDIS_RATE_POST = "rate:post:";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final int USER_STATUS_NORMAL = 1;
    public static final int USER_STATUS_BANNED = 0;
    public static final int USER_STATUS_DELETED = 2;

    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";
    public static final String GENDER_SECRET = "SECRET";

    public static final int POST_STATUS_DELETED = 0;
    public static final int POST_STATUS_NORMAL = 1;
    public static final int POST_STATUS_DRAFT = 2;

    /** 精华帖热度阈值：浏览×0.1 + 点赞×2 + 收藏×2 + 回复×1.5 */
    public static final double FEATURED_HOT_THRESHOLD = 20.0;
    public static final int MAX_PROFILE_PINS = 3;

    /** 重复发帖检测：与最近帖标题/正文相似度达到该阈值时提示确认 */
    public static final double POST_SIMILAR_THRESHOLD = 0.85;
    public static final int POST_SIMILAR_LOOKBACK_MINUTES = 60;

    public static final String BOARD_TYPE_HOME = "HOME";
    public static final String BOARD_TYPE_ALL = "ALL";
    public static final String BOARD_TYPE_CATEGORY = "CATEGORY";
    public static final String BOARD_TYPE_NORMAL = "NORMAL";

    /** 独立大版块：无小版块，可直接发帖，标题即可（正文可选） */
    public static final String BOARD_INTERACTIVE_QA = "互动问答";

    public static final int REPORT_STATUS_PENDING = 0;
    public static final int REPORT_STATUS_HANDLED = 1;
    public static final int REPORT_STATUS_DISMISSED = 2;

    /** 回复自动删除：拉踩数达到该值且远高于点赞数时触发 */
    public static final int REPLY_MODERATION_MIN_DISLIKES = 3;
    public static final double REPLY_MODERATION_DISLIKE_RATIO = 2.0;

    private ForumConstants() {
    }
}
