CREATE TABLE IF NOT EXISTS `agent_runtime_run` (
  `run_id` bigint NOT NULL AUTO_INCREMENT COMMENT '运行ID',
  `run_type` varchar(64) NOT NULL COMMENT 'persona_report/companion_reply/companion_strategy',
  `owner_user_id` bigint DEFAULT NULL COMMENT '主人用户ID',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `provider_code` varchar(64) DEFAULT NULL COMMENT '命中的provider',
  `profile_code` varchar(64) DEFAULT NULL COMMENT '命中的profile',
  `status` varchar(32) NOT NULL DEFAULT 'success' COMMENT '运行状态',
  `request_json` longtext COMMENT '请求快照',
  `response_json` longtext COMMENT '响应快照',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`run_id`),
  KEY `idx_run_type_time` (`run_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent运行记录';

CREATE TABLE IF NOT EXISTS `agent_memory_profile` (
  `memory_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记忆ID',
  `owner_user_id` bigint NOT NULL COMMENT '主人用户ID',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `memory_scope` varchar(32) NOT NULL DEFAULT 'relationship' COMMENT 'global/relationship',
  `stage_code` varchar(32) DEFAULT NULL COMMENT '关系阶段',
  `summary` varchar(2000) DEFAULT NULL COMMENT '摘要',
  `preferences_json` longtext COMMENT '偏好JSON',
  `taboo_json` longtext COMMENT '禁忌JSON',
  `signals_json` longtext COMMENT '信号JSON',
  `version_no` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`memory_id`),
  UNIQUE KEY `uk_owner_target_scope` (`owner_user_id`, `target_user_id`, `memory_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系记忆画像';

CREATE TABLE IF NOT EXISTS `agent_memory_fact` (
  `fact_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事实ID',
  `owner_user_id` bigint NOT NULL COMMENT '主人用户ID',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `fact_type` varchar(64) NOT NULL COMMENT 'preference/taboo/promise/topic',
  `fact_key` varchar(128) DEFAULT NULL COMMENT '事实键',
  `fact_value` varchar(1000) NOT NULL COMMENT '事实值',
  `confidence_score` decimal(5,2) NOT NULL DEFAULT 0.50 COMMENT '置信度',
  `source_type` varchar(64) DEFAULT NULL COMMENT 'chat/post/manual',
  `source_ref` varchar(255) DEFAULT NULL COMMENT '来源引用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`fact_id`),
  KEY `idx_owner_target_type` (`owner_user_id`, `target_user_id`, `fact_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记忆事实表';

CREATE TABLE IF NOT EXISTS `agent_persona_report_v2` (
  `report_id` bigint NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` bigint NOT NULL COMMENT '目标用户ID',
  `report_scene` varchar(64) NOT NULL DEFAULT 'dating_companion' COMMENT '报告场景',
  `summary` varchar(2000) DEFAULT NULL COMMENT '一句话摘要',
  `report_json` longtext NOT NULL COMMENT '完整结构化报告',
  `evidence_json` longtext COMMENT '证据摘要',
  `provider_code` varchar(64) DEFAULT NULL COMMENT 'provider',
  `profile_code` varchar(64) DEFAULT NULL COMMENT 'profile',
  `version_no` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`report_id`),
  KEY `idx_user_scene_time` (`user_id`, `report_scene`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物画像报告V2';

CREATE TABLE IF NOT EXISTS `agent_action_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `owner_user_id` bigint NOT NULL COMMENT '主人用户ID',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `plan_type` varchar(64) NOT NULL COMMENT 'reply/strategy/moment',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/pending_approval/executed/cancelled',
  `plan_json` longtext NOT NULL COMMENT '计划JSON',
  `risk_level` varchar(16) DEFAULT 'low' COMMENT 'low/medium/high',
  `requires_approval` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否需要审批',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`plan_id`),
  KEY `idx_owner_target_plan` (`owner_user_id`, `target_user_id`, `plan_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent动作计划表';

CREATE TABLE IF NOT EXISTS `agent_runtime_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` varchar(64) NOT NULL COMMENT '调试trace_id',
  `agent_type` varchar(32) NOT NULL COMMENT 'persona/matchmaker/companion',
  `scene_code` varchar(64) NOT NULL COMMENT 'scene code',
  `function_type` varchar(64) NOT NULL COMMENT 'function type',
  `owner_user_id` bigint DEFAULT NULL COMMENT '主人用户ID',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `provider_code` varchar(64) DEFAULT NULL COMMENT '命中provider',
  `profile_code` varchar(64) DEFAULT NULL COMMENT '命中profile',
  `status` varchar(32) NOT NULL DEFAULT 'success' COMMENT '运行状态',
  `request_summary` varchar(1000) DEFAULT NULL COMMENT '请求摘要',
  `response_summary` varchar(1000) DEFAULT NULL COMMENT '响应摘要',
  `retrieval_hits_json` longtext COMMENT '召回命中',
  `graph_facts_json` longtext COMMENT '图谱事实',
  `reasoning_summary` varchar(2000) DEFAULT NULL COMMENT '推理摘要',
  `timing_breakdown_json` longtext COMMENT '耗时拆解',
  `request_json` longtext COMMENT '请求快照',
  `response_json` longtext COMMENT '响应快照',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace_id` (`trace_id`),
  KEY `idx_agent_time` (`agent_type`, `created_at`),
  KEY `idx_owner_time` (`owner_user_id`, `created_at`),
  KEY `idx_target_time` (`target_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一Agent调试Trace';
