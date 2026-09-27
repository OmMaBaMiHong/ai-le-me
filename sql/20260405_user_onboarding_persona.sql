CREATE TABLE IF NOT EXISTS `user_onboarding_persona` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `answers_json` longtext COMMENT '首登问题回答JSON',
  `persona_json` longtext COMMENT '首登蒸馏画像JSON',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `confirmed` tinyint NOT NULL DEFAULT 0 COMMENT '是否确认应用到资料 0否 1是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0删除 1正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_onboarding_persona_user` (`user_id`),
  KEY `idx_user_onboarding_persona_status` (`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户首登关系画像草稿';
