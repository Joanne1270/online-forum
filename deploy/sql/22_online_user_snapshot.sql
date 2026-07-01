USE user_db;

CREATE TABLE IF NOT EXISTS online_user_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    granularity VARCHAR(16) NOT NULL COMMENT 'HOUR, DAY, HALF_MONTH',
    snapshot_at DATETIME NOT NULL COMMENT 'bucket start time',
    online_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_snapshot (granularity, snapshot_at),
    KEY idx_granularity_time (granularity, snapshot_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
