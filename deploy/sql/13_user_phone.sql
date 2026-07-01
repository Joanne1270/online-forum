USE user_db;

ALTER TABLE user
    ADD COLUMN phone VARCHAR(11) NULL COMMENT '手机号' AFTER id;

UPDATE user SET phone = '13800000001' WHERE username = 'admin' AND (phone IS NULL OR phone = '');
UPDATE user SET phone = '13800000002' WHERE username = 'demo' AND (phone IS NULL OR phone = '');
UPDATE user SET phone = CONCAT('1390000', LPAD(id, 4, '0')) WHERE phone IS NULL OR phone = '';

ALTER TABLE user
    MODIFY phone VARCHAR(11) NOT NULL,
    ADD UNIQUE KEY uk_phone (phone);

ALTER TABLE user
    ADD UNIQUE KEY uk_nickname (nickname);

ALTER TABLE user
    DROP INDEX username,
    DROP COLUMN username;
