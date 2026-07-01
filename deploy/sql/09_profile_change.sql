USE user_db;

CREATE TABLE IF NOT EXISTS profile_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    field_type VARCHAR(20) NOT NULL COMMENT 'NICKNAME/AVATAR',
    old_value VARCHAR(255) DEFAULT '',
    new_value VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2拒绝',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at DATETIME DEFAULT NULL,
    INDEX idx_user_field_date (user_id, field_type, created_at),
    INDEX idx_status (status)
);
