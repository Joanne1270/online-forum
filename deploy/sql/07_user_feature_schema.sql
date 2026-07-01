USE user_db;
SET NAMES utf8mb4;

-- Idempotent schema updates for user-service features beyond 01_user_db.sql.

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receiver_read (receiver_id, read_flag),
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, followee_id),
    INDEX idx_followee (followee_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS profile_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    field_type VARCHAR(20) NOT NULL COMMENT 'NICKNAME/EMAIL等',
    old_value VARCHAR(255) DEFAULT '',
    new_value VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2拒绝',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME DEFAULT NULL,
    INDEX idx_user_field (user_id, field_type),
    INDEX idx_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS online_user_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    granularity VARCHAR(20) NOT NULL COMMENT 'HOUR/DAY等',
    snapshot_at DATETIME NOT NULL,
    online_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_granularity_snapshot (granularity, snapshot_at)
) ENGINE=InnoDB;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'gender'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN gender VARCHAR(10) NOT NULL DEFAULT ''SECRET'' AFTER bio',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'birth_month'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN birth_month DATE DEFAULT NULL AFTER gender',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'privacy_posts'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN privacy_posts TINYINT NOT NULL DEFAULT 0 COMMENT ''0公开 1仅自己'' AFTER banned_until',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'privacy_favorites'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN privacy_favorites TINYINT NOT NULL DEFAULT 0 AFTER privacy_posts',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'privacy_replies'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN privacy_replies TINYINT NOT NULL DEFAULT 0 AFTER privacy_favorites',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'privacy_following'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN privacy_following TINYINT NOT NULL DEFAULT 0 AFTER privacy_replies',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'user_db' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'privacy_followers'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE user ADD COLUMN privacy_followers TINYINT NOT NULL DEFAULT 0 AFTER privacy_following',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
