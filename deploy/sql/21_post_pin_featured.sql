USE post_db;

ALTER TABLE post
  ADD COLUMN featured TINYINT NOT NULL DEFAULT 0 COMMENT '是否精华',
  ADD COLUMN featured_at DATETIME NULL COMMENT '成为精华时间',
  ADD COLUMN official_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '官方公告置顶',
  ADD COLUMN official_pinned_at DATETIME NULL COMMENT '官方置顶时间',
  ADD COLUMN pinned_reply_id BIGINT NULL COMMENT '楼主置顶回复';

CREATE TABLE IF NOT EXISTS user_profile_pin (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_post (user_id, post_id),
  KEY idx_user (user_id)
);
