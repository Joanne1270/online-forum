USE post_db;
SET NAMES utf8mb4;

ALTER TABLE board ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '大版块ID' AFTER board_type;
ALTER TABLE board ADD INDEX idx_parent (parent_id);

UPDATE board SET board_type = 'CATEGORY' WHERE id BETWEEN 3 AND 10;

INSERT INTO board (id, name, description, board_type, parent_id, sort_order) VALUES
(101, '摄影', '摄影技巧与作品分享', 'NORMAL', 3, 1),
(102, '影视动漫', '电影、剧集、动漫讨论', 'NORMAL', 3, 2),
(103, '游戏娱乐', '游戏攻略与娱乐话题', 'NORMAL', 3, 3),
(104, '体育运动', '运动健身与赛事', 'NORMAL', 3, 4),
(111, '学习交流', '课程学习与经验', 'NORMAL', 4, 1),
(112, '求职实习', '求职面试与实习', 'NORMAL', 4, 2),
(113, '考研留学', '升学与留学规划', 'NORMAL', 4, 3),
(121, '二手交易', '闲置物品交易', 'NORMAL', 5, 1),
(122, '技能服务', '技能互换与服务', 'NORMAL', 5, 2),
(131, '官方活动', '社区官方活动', 'NORMAL', 6, 1),
(132, '福利分享', '福利信息与互助', 'NORMAL', 6, 2),
(141, '情感交流', '情感话题讨论', 'NORMAL', 7, 1),
(142, '倾诉树洞', '匿名倾诉与陪伴', 'NORMAL', 7, 2),
(151, '学习资源', '资料与教程分享', 'NORMAL', 8, 1),
(152, '工具软件', '软件工具推荐', 'NORMAL', 8, 2),
(161, '同城交友', '同城活动与交友', 'NORMAL', 9, 1),
(162, '兴趣圈子', '同好圈子交流', 'NORMAL', 9, 2),
(171, '综合杂谈', '不便分类的话题', 'NORMAL', 10, 1);

UPDATE post SET board_id = 101 WHERE board_id = 3;
UPDATE post SET board_id = 111 WHERE board_id = 4;
UPDATE post SET board_id = 121 WHERE board_id = 5;
UPDATE post SET board_id = 131 WHERE board_id = 6;
UPDATE post SET board_id = 141 WHERE board_id = 7;
UPDATE post SET board_id = 151 WHERE board_id = 8;
UPDATE post SET board_id = 161 WHERE board_id = 9;
UPDATE post SET board_id = 171 WHERE board_id = 10;
