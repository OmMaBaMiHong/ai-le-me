CREATE TABLE IF NOT EXISTS `agent_relationship_event` (
  `event_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `owner_user_id` bigint NOT NULL COMMENT '主人用户ID',
  `target_user_id` bigint NOT NULL COMMENT '目标用户ID',
  `event_type` varchar(64) NOT NULL COMMENT 'incoming_message/date_invite_accepted/wechat_exchange_confirmed',
  `source_type` varchar(32) NOT NULL DEFAULT 'java' COMMENT 'java/runtime/app',
  `event_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `event_json` longtext COMMENT '事件载荷',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`event_id`),
  KEY `idx_owner_target_event` (`owner_user_id`, `target_user_id`, `event_type`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系事件流';

CREATE TABLE IF NOT EXISTS `agent_relationship_state` (
  `state_id` bigint NOT NULL AUTO_INCREMENT COMMENT '状态ID',
  `owner_user_id` bigint NOT NULL COMMENT '主人用户ID',
  `target_user_id` bigint NOT NULL COMMENT '目标用户ID',
  `current_stage` varchar(32) NOT NULL DEFAULT 'early' COMMENT '关系阶段',
  `master_style_code` varchar(64) NOT NULL DEFAULT 'steady_partner' COMMENT '当前恋爱大师风格',
  `heat_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分',
  `trust_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '信任分',
  `date_ready_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '邀约就绪度',
  `wechat_ready_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '微信交换就绪度',
  `risk_score` decimal(6,4) NOT NULL DEFAULT 0.0000 COMMENT '风险分',
  `state_json` longtext COMMENT '完整状态快照',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`state_id`),
  UNIQUE KEY `uk_owner_target_state` (`owner_user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系状态快照';

CREATE TABLE IF NOT EXISTS `agent_workflow_checkpoint` (
  `checkpoint_id` bigint NOT NULL AUTO_INCREMENT COMMENT '检查点ID',
  `workflow_run_id` varchar(64) NOT NULL COMMENT '工作流运行ID',
  `owner_user_id` bigint NOT NULL COMMENT '主人用户ID',
  `target_user_id` bigint NOT NULL COMMENT '目标用户ID',
  `trigger_code` varchar(64) NOT NULL COMMENT '触发器编码',
  `pending_node` varchar(64) NOT NULL DEFAULT '' COMMENT '当前等待节点',
  `resume_token` varchar(128) DEFAULT NULL COMMENT '恢复令牌',
  `status` varchar(32) NOT NULL DEFAULT 'waiting' COMMENT 'waiting/running/done/failed',
  `graph_state_json` longtext COMMENT '工作流状态图',
  `result_json` longtext COMMENT '结果JSON',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`checkpoint_id`),
  KEY `idx_workflow_run` (`workflow_run_id`),
  KEY `idx_owner_target_status` (`owner_user_id`, `target_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自治工作流检查点';

CREATE TABLE IF NOT EXISTS `agent_coach_style_profile` (
  `style_id` bigint NOT NULL AUTO_INCREMENT COMMENT '风格ID',
  `style_code` varchar(64) NOT NULL COMMENT '风格编码',
  `style_name` varchar(64) NOT NULL COMMENT '风格名称',
  `constraints_json` longtext COMMENT '约束条件',
  `prompt_json` longtext COMMENT '提示词片段',
  `risk_notes_json` longtext COMMENT '风险备注',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`style_id`),
  UNIQUE KEY `uk_style_code` (`style_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱大师风格库';
