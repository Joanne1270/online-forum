USE user_db;
SET NAMES utf8mb4;

ALTER TABLE user
    ADD COLUMN privacy_following TINYINT NOT NULL DEFAULT 1 COMMENT '1公开 0隐藏' AFTER privacy_replies,
    ADD COLUMN privacy_followers TINYINT NOT NULL DEFAULT 1 COMMENT '1公开 0隐藏' AFTER privacy_following;
