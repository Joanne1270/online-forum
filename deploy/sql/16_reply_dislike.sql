USE post_db;
SET NAMES utf8mb4;

ALTER TABLE reply
    ADD COLUMN dislike_count INT NOT NULL DEFAULT 0 COMMENT '拉踩数' AFTER like_count;

CREATE TABLE IF NOT EXISTS reply_dislike (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reply_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reply_user (reply_id, user_id)
) ENGINE=InnoDB;
