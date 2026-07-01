USE post_db;

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
);
