-- AI视频模板场景分类扩展
-- 执行时间：2026-02-27

-- 1. video_template 加 category 字段
ALTER TABLE video_template ADD COLUMN category varchar(30) DEFAULT 'self_intro'
  COMMENT '场景分类：self_intro/dating/confession/daily' AFTER code;
ALTER TABLE video_template ADD INDEX idx_category_status (category, status);

-- 2. 已有模板补 category
UPDATE video_template SET category = 'self_intro';

-- 3. 新增场景模板
INSERT INTO video_template (name, code, category, description, prompt_template, style_preset, duration_range, max_images, status, sort) VALUES
('浪漫约会邀请', 'dating_invite_romantic', 'dating', '邀请TA赴一场浪漫约会',
 '一段浪漫的约会邀请视频：{username}，{age}岁，想邀请你一起去{place}。{message}',
 'romantic', '15-25', 3, 1, 10),
('追妻火葬场', 'confession_intense', 'confession', '深情挽回/追求的高能告白',
 '一段深情告白视频：{username}对你说——{message}。{loveDeclaration}',
 'romantic', '20-30', 5, 1, 20),
('甜蜜表白', 'confession_sweet', 'confession', '温柔甜蜜的告白视频',
 '一段温柔的表白：{username}想对你说——{message}。期待与你{loveDeclaration}',
 'elegant', '15-20', 4, 1, 21),
('周末约饭', 'dating_dinner', 'dating', '轻松愉快的约饭邀请',
 '嗨！{username}邀请你周末一起去{place}吃饭！{message}',
 'energetic', '10-15', 2, 1, 11),
('日常心情', 'daily_mood', 'daily', '分享今日心情和生活',
 '{username}的今日分享：{message}。来自{city}的{gender}生，{interest}',
 'natural', '10-20', 4, 1, 30);
