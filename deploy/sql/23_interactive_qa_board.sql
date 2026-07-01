-- 独立大版块「互动问答」：无小版块，可直接发帖，标题即可（正文可选）
USE post_db;
SET NAMES utf8mb4;

INSERT INTO board (id, name, description, board_type, parent_id, sort_order) VALUES
(12, '互动问答', '提问互动，标题即可发布问题', 'NORMAL', NULL, 12)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  board_type = VALUES(board_type),
  parent_id = VALUES(parent_id),
  sort_order = VALUES(sort_order);
