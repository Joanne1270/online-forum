USE user_db;

ALTER TABLE user
    ADD COLUMN privacy_posts TINYINT NOT NULL DEFAULT 1 COMMENT '1公开 0隐藏' AFTER bio,
    ADD COLUMN privacy_favorites TINYINT NOT NULL DEFAULT 1 COMMENT '1公开 0隐藏' AFTER privacy_posts,
    ADD COLUMN privacy_replies TINYINT NOT NULL DEFAULT 1 COMMENT '1公开 0隐藏' AFTER privacy_favorites;
