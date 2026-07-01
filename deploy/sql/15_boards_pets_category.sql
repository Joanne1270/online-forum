-- Adjust category order and add 绿植养宠 (incremental, keeps existing board ids).
USE post_db;
SET NAMES utf8mb4;

UPDATE board SET sort_order = 3 WHERE id = 4;
UPDATE board SET sort_order = 4 WHERE id = 5;
UPDATE board SET sort_order = 5 WHERE id = 6;
UPDATE board SET sort_order = 6 WHERE id = 7;
UPDATE board SET sort_order = 7 WHERE id = 8;
UPDATE board SET sort_order = 8 WHERE id = 9;
UPDATE board SET sort_order = 9 WHERE id = 10;
UPDATE board SET sort_order = 10 WHERE id = 3;

INSERT INTO board (id, name, description, board_type, parent_id, sort_order) VALUES
(11, '绿植养宠', '宠物饲养、花草绿植与好物分享', 'CATEGORY', NULL, 11),
(1101, '猫狗日常', '猫咪狗狗喂养、训练与日常', 'NORMAL', 11, 1),
(1102, '水族世界', '鱼缸造景、观赏鱼饲养', 'NORMAL', 11, 2),
(1103, '飞鸟家禽', '鹦鹉鸟类、鸡鸭等家禽', 'NORMAL', 11, 3),
(1104, '小众异宠', '爬宠、仓鼠、兔子等', 'NORMAL', 11, 4),
(1105, '花草绿植', '养花种草、多肉盆栽', 'NORMAL', 11, 5),
(1106, '宠植好物', '宠物用品、园艺工具测评', 'NORMAL', 11, 6)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    board_type = VALUES(board_type),
    parent_id = VALUES(parent_id),
    sort_order = VALUES(sort_order);
