-- AI视频生成功能数据库表
-- 执行时间：2026-02-13

-- 1. 扩展post表，添加AI视频标识
ALTER TABLE `post` ADD COLUMN `is_ai_video` tinyint DEFAULT 0 COMMENT '是否AI生成视频：0-否 1-是' AFTER `isPrivate`;
ALTER TABLE `post` ADD COLUMN `video_id` bigint DEFAULT NULL COMMENT '关联AI视频记录ID' AFTER `is_ai_video`;

-- 2. 创建视频模板表
CREATE TABLE IF NOT EXISTS `video_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `name` varchar(50) NOT NULL COMMENT '模板名称',
  `code` varchar(30) NOT NULL COMMENT '模板编码',
  `description` varchar(200) DEFAULT NULL COMMENT '模板描述',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '模板封面预览图',
  
  -- 模板配置
  `prompt_template` text NOT NULL COMMENT '提示词模板，使用{字段名}作为占位符',
  `style_preset` varchar(50) DEFAULT 'natural' COMMENT '风格预设：natural/romantic/energetic/elegant',
  `duration_range` varchar(20) DEFAULT '15-30' COMMENT '时长范围(秒)，如 15-30',
  `music_url` varchar(500) DEFAULT NULL COMMENT '背景音乐URL',
  
  -- 限制配置
  `max_images` int DEFAULT 5 COMMENT '最大使用图片数',
  `max_text_length` int DEFAULT 200 COMMENT '最大文字长度',
  
  -- 状态
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `sort` int DEFAULT 0 COMMENT '排序，越小越靠前',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频生成模板';

-- 3. 创建用户视频记录表
CREATE TABLE IF NOT EXISTS `user_video` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `uid` int NOT NULL COMMENT '用户ID',
  
  -- 关联帖子
  `post_id` int DEFAULT NULL COMMENT '关联的帖子ID（发布后填充）',
  
  -- 视频信息
  `video_type` tinyint NOT NULL DEFAULT 1 COMMENT '视频类型：1-自我介绍 2-相亲名片',
  `title` varchar(100) DEFAULT NULL COMMENT '视频标题',
  `template_code` varchar(30) DEFAULT NULL COMMENT '使用的模板编码',
  `video_url` varchar(500) DEFAULT NULL COMMENT '视频URL',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `duration` int DEFAULT NULL COMMENT '视频时长(秒)',
  
  -- 生成配置
  `prompt_text` text COMMENT '生成的提示词',
  `source_images` json COMMENT '素材图片列表JSON',
  `source_text` json COMMENT '素材文字JSON',
  `ai_model` varchar(50) DEFAULT NULL COMMENT 'AI模型标识：jimeng/kling/vidu',
  
  -- 状态管理
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-生成中 1-待发布 2-已发布 3-失败',
  `task_id` varchar(100) DEFAULT NULL COMMENT '第三方任务ID',
  `progress` int DEFAULT 0 COMMENT '生成进度百分比',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `retry_count` int DEFAULT 0 COMMENT '重试次数',
  
  -- 发布选项
  `auto_publish` tinyint DEFAULT 0 COMMENT '生成后自动发布：0-否 1-是',
  `post_content` varchar(500) DEFAULT NULL COMMENT '发布时的文案内容',
  
  -- 时间
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户AI生成视频';

-- 4. 初始化模板数据
INSERT INTO `video_template` (`name`, `code`, `description`, `prompt_template`, `style_preset`, `duration_range`, `max_images`, `status`, `sort`) VALUES
('清新自我介绍', 'self_intro_fresh', '清新自然风格，适合日常展示', 
 '一位{gender}性的自我介绍视频，{age}岁，职业是{job}，来自{city}。性格特点：{tags}。兴趣爱好：{interest}。希望认识更多志同道合的朋友。',
 'natural', '15-25', 5, 1, 1),

('浪漫相亲名片', 'dating_card_romantic', '浪漫温馨风格，适合相亲场景',
 '相亲名片：{username}，{age}岁{gender}性，{height}，{city}人。{education}学历，从事{job}工作。自我介绍：{selfIntro}。期待遇见：{loveDeclaration}',
 'romantic', '20-30', 5, 1, 2),

('活力青春', 'self_intro_youth', '充满活力的青春风格',
 '嗨，我是{username}！{age}岁的{gender}生，来自{city}。{education}毕业，现在是一名{job}。平时喜欢{interest}。性格{tags}，期待和你成为朋友！',
 'energetic', '15-20', 4, 1, 3),

('优雅知性', 'self_intro_elegant', '优雅知性风格，适合职场人士',
 '{username}，{age}岁，{city}。{education}，{job}。热爱{interest}。相信{loveDeclaration}',
 'elegant', '20-30', 5, 1, 4);

-- 5. 添加系统配置项
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_time`, `remark`) VALUES
('AI视频默认圈子ID', 'ai_video_default_topic', '1', 'Y', NOW(), 'AI生成视频发布时的默认圈子ID'),
('AI视频生成消耗积分', 'ai_video_integral', '10', 'Y', NOW(), '每次生成AI视频消耗的积分数量'),
('AI视频服务商', 'ai_video_provider', 'jimeng', 'Y', NOW(), 'AI视频服务商：jimeng/kling/vidu'),
('AI视频生成开关', 'ai_video_enabled', '1', 'Y', NOW(), 'AI视频生成功能开关：0-关闭 1-开启');
