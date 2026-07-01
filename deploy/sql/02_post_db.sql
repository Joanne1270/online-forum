CREATE DATABASE IF NOT EXISTS post_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE post_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS board (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) DEFAULT '',
    board_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'HOME/ALL/CATEGORY/NORMAL',
    parent_id BIGINT DEFAULT NULL COMMENT '大版块ID',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    board_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    reply_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_board_created (board_id, created_at),
    INDEX idx_author (author_id),
    FULLTEXT INDEX ft_title_content (title, content)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post (post_id),
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reply_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reply_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reply_user (reply_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mention (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT DEFAULT NULL,
    reply_id BIGINT DEFAULT NULL,
    mentioned_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mentioned (mentioned_user_id)
) ENGINE=InnoDB;

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

INSERT INTO board (id, name, description, board_type, parent_id, sort_order) VALUES
(1, '首页·官方公告', '平台官方公告、反馈与活动', 'HOME', NULL, 1),
(2, '综合讨论（全部）', '汇总所有版块的帖子', 'ALL', NULL, 2),

(4, '生活日常', '美食、家居、穿搭与养生', 'CATEGORY', NULL, 3),
(5, '情感交流', '恋爱、家庭、树洞与交友', 'CATEGORY', NULL, 4),
(6, '学习提升', '学生、职场、技能与读书', 'CATEGORY', NULL, 5),
(7, '休闲娱乐', '影视、游戏、音乐与手作', 'CATEGORY', NULL, 6),
(8, '城市同城', '本地生活、便民与同城活动', 'CATEGORY', NULL, 7),
(9, '数码科技', '手机电脑、DIY、资讯与软件', 'CATEGORY', NULL, 8),
(10, '交易便民', '闲置、拼团、求购与避雷', 'CATEGORY', NULL, 9),
(3, '育儿母婴', '孕产、育儿、母婴好物与宝妈交流', 'CATEGORY', NULL, 10),
(11, '绿植养宠', '宠物饲养、花草绿植与好物分享', 'CATEGORY', NULL, 11),

(101, '公告通知', '平台更新、活动通知、规则公示', 'NORMAL', 1, 1),
(102, '问题反馈', 'BUG上报、投诉举报、账号问题', 'NORMAL', 1, 2),
(103, '活动中心', '征文、晒图、打卡等各类平台活动', 'NORMAL', 1, 3),

(301, '孕产日记', '备孕交流、孕期日常、产后月子', 'NORMAL', 3, 1),
(302, '宝贝成长', '0-1岁护理、1-6岁早教、学龄育儿', 'NORMAL', 3, 2),
(303, '好物分享', '母婴用品测评、遛娃攻略、辅食食谱', 'NORMAL', 3, 3),
(304, '宝妈闲聊', '产后修复、婆媳夫妻、情绪树洞', 'NORMAL', 3, 4),
(305, '便民集市', '育儿问答、母婴闲置交换', 'NORMAL', 3, 5),

(401, '美食厨房', '家常菜谱、烘焙甜品、外卖探店', 'NORMAL', 4, 1),
(402, '家居家装', '租房改造、软装搭配、家电测评', 'NORMAL', 4, 2),
(403, '穿搭美妆', '平价穿搭、护肤彩妆、发型分享', 'NORMAL', 4, 3),
(404, '健身养生', '减脂塑形、养生食疗、体态改善', 'NORMAL', 4, 4),

(501, '恋爱交友', '脱单分享、情侣矛盾、恋爱技巧', 'NORMAL', 5, 1),
(502, '婚姻家庭', '夫妻相处、家庭琐事、二胎日常', 'NORMAL', 5, 2),
(503, '心事树洞', '情绪倾诉、压力缓解、人际困惑', 'NORMAL', 5, 3),
(504, '兴趣交友', '同好搭子、线下结伴', 'NORMAL', 5, 4),

(601, '学生专区', '中小学辅导、考研考公、考证干货', 'NORMAL', 6, 1),
(602, '职场干货', '求职面试、升职加薪、职场避坑', 'NORMAL', 6, 2),
(603, '技能自学', '编程设计、剪辑摄影、语言学习', 'NORMAL', 6, 3),
(604, '读书观影', '好书推荐、影视剧评、书单整理', 'NORMAL', 6, 4),

(701, '影视动漫', '新剧讨论、动漫番剧、影视解析', 'NORMAL', 7, 1),
(702, '游戏天地', '手游端游攻略、游戏吐槽、组队开黑', 'NORMAL', 7, 2),
(703, '影音音乐', '新歌分享、歌手讨论、歌单推荐', 'NORMAL', 7, 3),
(704, '兴趣手作', '手工DIY、绘画、周边收藏', 'NORMAL', 7, 4),

(801, '本地吃喝玩乐', '探店、景点、平价餐厅', 'NORMAL', 8, 1),
(802, '本地便民', '租房买房、家政、本地商家避雷', 'NORMAL', 8, 2),
(803, '同城活动', '线下聚会、运动搭子、亲子线下', 'NORMAL', 8, 3),
(804, '本地问答', '交通、学校、医院咨询', 'NORMAL', 8, 4),

(901, '手机数码', '手机电脑测评、配件推荐', 'NORMAL', 9, 1),
(902, '数码DIY', '装机、相机、剪辑设备', 'NORMAL', 9, 2),
(903, '科技资讯', '新品爆料、数码避坑', 'NORMAL', 9, 3),
(904, '软件工具', '实用APP、电脑软件分享', 'NORMAL', 9, 4),

(1001, '闲置转让', '数码、衣物、书籍、母婴闲置', 'NORMAL', 10, 1),
(1002, '好物拼团', '团购优惠、海淘分享', 'NORMAL', 10, 2),
(1003, '求购专区', '求二手、求代购', 'NORMAL', 10, 3),
(1004, '避雷曝光', '黑心商家、假货吐槽', 'NORMAL', 10, 4),

(1101, '猫狗日常', '猫咪狗狗喂养、训练与日常', 'NORMAL', 11, 1),
(1102, '水族世界', '鱼缸造景、观赏鱼饲养', 'NORMAL', 11, 2),
(1103, '飞鸟家禽', '鹦鹉鸟类、鸡鸭等家禽', 'NORMAL', 11, 3),
(1104, '小众异宠', '爬宠、仓鼠、兔子等', 'NORMAL', 11, 4),
(1105, '花草绿植', '养花种草、多肉盆栽', 'NORMAL', 11, 5),
(1106, '宠植好物', '宠物用品、园艺工具测评', 'NORMAL', 11, 6);
