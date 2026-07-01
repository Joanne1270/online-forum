USE post_db;
SET NAMES utf8mb4;

-- Legacy migration: most objects now live in 02_post_db.sql.
-- Keep idempotent so fresh docker init does not fail on duplicate columns/tables.

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'post_db'
    AND TABLE_NAME = 'board'
    AND COLUMN_NAME = 'board_type'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE board ADD COLUMN board_type VARCHAR(20) NOT NULL DEFAULT ''NORMAL'' COMMENT ''HOME/ALL/NORMAL'' AFTER description',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS post_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已删帖 2已忽略',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME DEFAULT NULL,
    UNIQUE KEY uk_post_reporter (post_id, reporter_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;

UPDATE post SET like_count = GREATEST(0, like_count);
UPDATE reply SET like_count = GREATEST(0, like_count);
