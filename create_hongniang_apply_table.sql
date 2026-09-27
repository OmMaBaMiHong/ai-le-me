-- 创建红娘申请表
CREATE TABLE IF NOT EXISTS `hongniang_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `wechat` varchar(50) DEFAULT NULL COMMENT '微信号',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `city` varchar(50) DEFAULT NULL COMMENT '所在城市',
  `introduction` text COMMENT '个人简介',
  `skill_tags` varchar(255) DEFAULT NULL COMMENT '擅长领域（多选，逗号分隔）',
  `apply_reason` text COMMENT '申请理由',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '申请状态：0-待审核 1-已通过 2-已拒绝',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL COMMENT '申请时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红娘申请表';
