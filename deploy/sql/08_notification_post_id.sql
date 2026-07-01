USE notification_db;

ALTER TABLE notification
    ADD COLUMN post_id BIGINT DEFAULT NULL COMMENT '关联帖子ID' AFTER ref_type;

UPDATE notification SET post_id = ref_id WHERE ref_type = 'POST' AND post_id IS NULL;
