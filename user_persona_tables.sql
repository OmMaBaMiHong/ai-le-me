-- AI人物画像功能数据库表
-- 执行时间：2026-02-14

-- 1. 用户画像快照表（存储每次生成的画像）
CREATE TABLE IF NOT EXISTS `user_persona_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '画像快照ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `persona_json` text COMMENT '画像结构化数据（JSON格式）',
  `image_url` varchar(500) DEFAULT NULL COMMENT '画像图片URL',
  `summary` varchar(200) DEFAULT NULL COMMENT '一句话总结',
  `version_no` int DEFAULT 1 COMMENT '版本号',
  `source` varchar(50) DEFAULT 'manual_generate' COMMENT '生成来源：manual_generate/from_ai_video',
  `visible_to_self` tinyint DEFAULT 1 COMMENT '是否对本人可见：0-否 1-是',
  `visible_to_others` tinyint DEFAULT 1 COMMENT '是否对他人可见：0-否 1-是',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-已删除 1-正常',
  `generated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_generated_at` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像快照表';

-- 2. 画像查看记录表（用于付费解锁他人画像）
CREATE TABLE IF NOT EXISTS `persona_view_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `viewer_id` bigint NOT NULL COMMENT '查看者ID',
  `target_user_id` bigint NOT NULL COMMENT '被查看者ID',
  `snapshot_id` bigint NOT NULL COMMENT '画像快照ID',
  `pay_type` varchar(20) DEFAULT 'integral' COMMENT '支付方式：integral-积分 cash-现金',
  `pay_amount` decimal(10,2) DEFAULT 0.00 COMMENT '支付金额（积分或现金）',
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-已过期 1-有效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_viewer_target` (`viewer_id`, `target_user_id`),
  KEY `idx_target_user` (`target_user_id`),
  KEY `idx_snapshot` (`snapshot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像查看记录表';

-- 3. 画像生成配置表（系统配置）
CREATE TABLE IF NOT EXISTS `persona_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_desc` varchar(200) DEFAULT NULL COMMENT '配置描述',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像生成配置表';

-- 4. 插入初始配置
INSERT INTO `persona_config` (`config_key`, `config_value`, `config_desc`) VALUES
('persona_generate_cost_integral', '100', 'VIP会员外，生成画像消耗积分'),
('persona_view_cost_integral', '50', '查看他人画像消耗积分'),
('persona_vip_free_times_monthly', '2', 'VIP会员每月免费生成次数'),
('persona_enabled', '1', '画像功能总开关：0-关闭 1-开启'),
('persona_ai_provider', 'tongyi', 'AI服务商：tongyi/zhipu/openai'),
('persona_image_template', 'glassmorphism_v1', '画像图片模板版本');

-- 5. 扩展用户VIP表（记录VIP用户的画像生成次数）
ALTER TABLE `app_user` 
ADD COLUMN IF NOT EXISTS `persona_generate_count` int DEFAULT 0 COMMENT '本月已生成画像次数' AFTER `vip_expire_time`;

ALTER TABLE `app_user` 
ADD COLUMN IF NOT EXISTS `persona_last_reset_month` varchar(7) DEFAULT NULL COMMENT '画像次数上次重置月份（YYYY-MM）' AFTER `persona_generate_count`;
