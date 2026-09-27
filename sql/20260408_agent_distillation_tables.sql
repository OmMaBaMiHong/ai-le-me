CREATE TABLE IF NOT EXISTS `agent_distillation_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '发起蒸馏的用户ID',
  `scene_type` varchar(64) NOT NULL COMMENT '场景类型 self_bootstrap/private_person_analysis',
  `subject_type` varchar(64) DEFAULT NULL COMMENT '主体类型 self/private_person',
  `subject_name` varchar(128) NOT NULL COMMENT '主体名称或代号',
  `relation_label` varchar(64) DEFAULT NULL COMMENT '关系标签',
  `last_snapshot_id` bigint DEFAULT NULL COMMENT '最近一次快照ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_distillation_subject` (`user_id`, `scene_type`, `subject_name`),
  KEY `idx_agent_distillation_subject_status` (`user_id`, `status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物蒸馏主体表';

CREATE TABLE IF NOT EXISTS `agent_distillation_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '发起蒸馏的用户ID',
  `subject_id` bigint NOT NULL COMMENT '蒸馏主体ID',
  `scene_type` varchar(64) NOT NULL COMMENT '场景类型',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `analysis_goal` varchar(500) DEFAULT NULL COMMENT '分析目标',
  `preview_json` longtext COMMENT '前端可直接消费的蒸馏结果JSON',
  `material_count` int NOT NULL DEFAULT 0 COMMENT '本次素材数',
  `source` varchar(64) DEFAULT 'app_distillation' COMMENT '来源',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0删除 1正常',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_distillation_snapshot_latest` (`user_id`, `scene_type`, `status`, `generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物蒸馏快照表';

CREATE TABLE IF NOT EXISTS `agent_distillation_material` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint NOT NULL COMMENT '蒸馏主体ID',
  `snapshot_id` bigint NOT NULL COMMENT '蒸馏快照ID',
  `material_type` varchar(64) DEFAULT NULL COMMENT '素材类型 text_note/chat_screenshot 等',
  `label` varchar(128) DEFAULT NULL COMMENT '素材标签',
  `content` longtext COMMENT '文本内容',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件地址',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0删除 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_distillation_material_snapshot` (`snapshot_id`, `status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物蒸馏素材表';
