USE post_db;
SET NAMES utf8mb4;

-- Idempotent schema updates for features added after 02_post_db.sql.
-- Safe to run on existing databases (including after partial docker init).

CREATE TABLE IF NOT EXISTS banned_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_post_tag (post_id, tag_id),
    INDEX idx_tag (tag_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reply_dislike (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reply_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reply_user (reply_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reply_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reply_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2已忽略',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME DEFAULT NULL,
    UNIQUE KEY uk_reply_reporter (reply_id, reporter_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_profile_pin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_post (user_id, post_id),
    INDEX idx_user_sort (user_id, sort_order)
) ENGINE=InnoDB;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db' AND TABLE_NAME = 'reply' AND COLUMN_NAME = 'dislike_count'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE reply ADD COLUMN dislike_count INT NOT NULL DEFAULT 0 AFTER like_count',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db' AND TABLE_NAME = 'post' AND COLUMN_NAME = 'featured'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE post ADD COLUMN featured TINYINT NOT NULL DEFAULT 0 AFTER status',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db' AND TABLE_NAME = 'post' AND COLUMN_NAME = 'featured_at'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE post ADD COLUMN featured_at DATETIME DEFAULT NULL AFTER featured',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db' AND TABLE_NAME = 'post' AND COLUMN_NAME = 'official_pinned'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE post ADD COLUMN official_pinned TINYINT NOT NULL DEFAULT 0 AFTER featured_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db' AND TABLE_NAME = 'post' AND COLUMN_NAME = 'official_pinned_at'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE post ADD COLUMN official_pinned_at DATETIME DEFAULT NULL AFTER official_pinned',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db' AND TABLE_NAME = 'post' AND COLUMN_NAME = 'pinned_reply_id'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE post ADD COLUMN pinned_reply_id BIGINT DEFAULT NULL AFTER official_pinned_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
