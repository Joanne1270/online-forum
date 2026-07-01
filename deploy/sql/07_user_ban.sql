USE user_db;

ALTER TABLE user ADD COLUMN banned_until DATETIME DEFAULT NULL COMMENT '封禁截止时间，NULL表示永久' AFTER status;
