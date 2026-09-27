CREATE TABLE IF NOT EXISTS `agent_target_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_user_id` int NOT NULL COMMENT '主人用户ID',
  `target_user_id` int NOT NULL COMMENT '目标用户ID',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用对象级自治',
  `source_type` varchar(32) NOT NULL DEFAULT 'existing_chat' COMMENT 'existing_chat/hongniang_recommendation/mutual_interest',
  `auto_reply_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '自动回复',
  `proactive_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '允许主动发起',
  `date_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '允许邀约推进',
  `wechat_eval_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '允许微信交换评估',
  `gift_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '允许礼物规划或执行',
  `quiet_hours_json` text COMMENT '静默时段',
  `action_limit_json` text COMMENT '每日动作上限',
  `policy_json` longtext COMMENT '扩展策略',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_target_setting_owner_target` (`owner_user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象级自治配置';

CREATE TABLE IF NOT EXISTS `agent_relationship_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_user_id` int NOT NULL COMMENT '主人用户ID',
  `target_user_id` int NOT NULL COMMENT '目标用户ID',
  `stage_code` varchar(32) NOT NULL DEFAULT 'early' COMMENT '关系阶段',
  `heat_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分',
  `trust_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '信任分',
  `date_ready_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '邀约就绪度',
  `wechat_ready_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '微信交换就绪度',
  `risk_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '风险分',
  `current_master_style_code` varchar(64) NOT NULL DEFAULT 'steady_partner' COMMENT '当前风格',
  `next_action_code` varchar(64) DEFAULT NULL COMMENT '建议下一动作',
  `next_action_summary` varchar(255) DEFAULT NULL COMMENT '动作摘要',
  `last_runtime_trace_id` varchar(64) DEFAULT NULL COMMENT '最近runtime trace',
  `snapshot_json` longtext COMMENT '完整快照',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_relationship_snapshot_owner_target` (`owner_user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系快照';

CREATE TABLE IF NOT EXISTS `agent_program_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `program_run_id` varchar(64) NOT NULL COMMENT '程序运行ID',
  `owner_user_id` int NOT NULL COMMENT '主人用户ID',
  `target_user_id` int NOT NULL COMMENT '目标用户ID',
  `trigger_code` varchar(64) NOT NULL COMMENT '触发器编码',
  `workflow_code` varchar(64) NOT NULL DEFAULT 'relationship_program' COMMENT '工作流编码',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/running/waiting_approval/succeeded/failed',
  `task_id` bigint DEFAULT NULL COMMENT '关联动作任务',
  `approval_id` bigint DEFAULT NULL COMMENT '关联审批单',
  `runtime_trace_id` varchar(64) DEFAULT NULL COMMENT 'runtime trace',
  `result_json` longtext COMMENT '运行结果',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_program_run_id` (`program_run_id`),
  KEY `idx_agent_program_run_owner_target` (`owner_user_id`, `target_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象级自治运行记录';

CREATE TABLE IF NOT EXISTS `agent_trigger_cursor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_user_id` int NOT NULL COMMENT '主人用户ID',
  `target_user_id` int NOT NULL COMMENT '目标用户ID',
  `session_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `last_message_id` bigint DEFAULT NULL COMMENT '最近消费消息ID',
  `last_event_time` datetime DEFAULT NULL COMMENT '最近事件时间',
  `last_round_count` int NOT NULL DEFAULT 0 COMMENT '最近回合数',
  `last_stage_code` varchar(32) DEFAULT NULL COMMENT '最近关系阶段',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_trigger_cursor_owner_target` (`owner_user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自治触发游标';
