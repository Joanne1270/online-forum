USE user_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL COMMENT '发送者',
    receiver_id BIGINT NOT NULL COMMENT '接收者',
    content VARCHAR(2000) NOT NULL COMMENT '消息内容',
    read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sender_receiver (sender_id, receiver_id),
    KEY idx_receiver_read (receiver_id, read_flag),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='用户私信';
