-- 标签画像与埋点底座初始化脚本
-- 执行库：app

CREATE TABLE IF NOT EXISTS `app_event_track` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` varchar(64) NOT NULL COMMENT '事件幂等ID',
  `event_name` varchar(64) NOT NULL COMMENT '事件名称',
  `module` varchar(64) DEFAULT NULL COMMENT '业务模块',
  `page` varchar(128) DEFAULT NULL COMMENT '页面标识',
  `uid` bigint DEFAULT NULL COMMENT '用户ID',
  `target_uid` bigint DEFAULT NULL COMMENT '目标用户ID',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务ID',
  `source_type` varchar(64) DEFAULT NULL COMMENT '来源类型',
  `client_ts` bigint DEFAULT NULL COMMENT '客户端时间戳',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `props_json` text COMMENT '扩展属性JSON',
  `event_date` date DEFAULT NULL COMMENT '事件自然日',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_uid_date` (`uid`, `event_date`),
  KEY `idx_event_name_date` (`event_name`, `event_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App事件埋点明细';

CREATE TABLE IF NOT EXISTS `app_event_metric_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `metric_date` date NOT NULL COMMENT '统计日期',
  `event_name` varchar(64) NOT NULL COMMENT '事件名称',
  `module` varchar(64) NOT NULL DEFAULT 'unknown' COMMENT '业务模块',
  `page` varchar(128) NOT NULL DEFAULT 'unknown' COMMENT '页面标识',
  `event_count` bigint NOT NULL DEFAULT 0 COMMENT '事件次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_metric` (`metric_date`, `event_name`, `module`, `page`),
  KEY `idx_metric_date` (`metric_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App事件埋点日聚合';

CREATE TABLE IF NOT EXISTS `user_persona_snapshot` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '画像快照ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `persona_json` longtext COMMENT '画像结构化JSON',
  `image_url` varchar(512) DEFAULT NULL COMMENT '画像图片URL',
  `summary` varchar(255) DEFAULT NULL COMMENT '一句话总结',
  `version_no` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `source` varchar(64) DEFAULT NULL COMMENT '生成来源',
  `visible_to_self` tinyint NOT NULL DEFAULT 1 COMMENT '本人可见',
  `visible_to_others` tinyint NOT NULL DEFAULT 1 COMMENT '他人可见',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `generated_at` datetime DEFAULT NULL COMMENT '生成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status_generated` (`user_id`, `status`, `generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像快照';

CREATE TABLE IF NOT EXISTS `persona_view_record` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `viewer_id` int NOT NULL COMMENT '查看者ID',
  `target_user_id` int NOT NULL COMMENT '被查看者ID',
  `snapshot_id` int NOT NULL COMMENT '画像快照ID',
  `pay_type` varchar(32) DEFAULT 'integral' COMMENT '支付方式：integral-积分 cash-现金',
  `pay_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `view_time` datetime DEFAULT NULL COMMENT '查看时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL永久）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-失效 1-有效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_viewer_target_status` (`viewer_id`, `target_user_id`, `status`),
  KEY `idx_target_status` (`target_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像查看解锁记录';

-- 推荐系统参数（若不存在可补）
-- track_enabled: 1
-- ai_tag_suggest_enabled: 1
-- persona_aggregate_enabled: 1
-- persona_report_cost_integral: 30
