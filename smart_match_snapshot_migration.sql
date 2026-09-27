CREATE TABLE IF NOT EXISTS `smart_match_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `feature_hash` varchar(64) NOT NULL COMMENT '特征哈希',
  `provider` varchar(64) DEFAULT NULL COMMENT '命中的推荐提供方',
  `insight` varchar(1000) DEFAULT NULL COMMENT '推荐提示语',
  `selected_cities_json` varchar(1000) DEFAULT NULL COMMENT '选中的城市JSON',
  `used_runtime` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否使用Python runtime',
  `candidate_count` int NOT NULL DEFAULT 0 COMMENT '候选数量',
  `version_no` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：0失效 1有效',
  `generated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_feature_time` (`user_id`, `feature_hash`, `generated_at`),
  KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能红娘推荐快照';

CREATE TABLE IF NOT EXISTS `smart_match_snapshot_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `snapshot_id` bigint NOT NULL COMMENT '快照ID',
  `owner_user_id` int NOT NULL COMMENT '所属用户ID',
  `target_user_id` int NOT NULL COMMENT '推荐对象ID',
  `rank_no` int NOT NULL DEFAULT 1 COMMENT '排序位次',
  `match_score` int NOT NULL DEFAULT 0 COMMENT '匹配分',
  `provider` varchar(64) DEFAULT NULL COMMENT '推荐来源',
  `candidate_json` longtext COMMENT '候选展示JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_target` (`snapshot_id`, `target_user_id`),
  KEY `idx_snapshot_rank` (`snapshot_id`, `rank_no`),
  KEY `idx_owner_target` (`owner_user_id`, `target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能红娘推荐快照明细';
