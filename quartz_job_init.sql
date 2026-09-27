USE bang_yi;

CREATE TABLE IF NOT EXISTS `sys_quartz_job` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(100) NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) NOT NULL DEFAULT 'SYSTEM' COMMENT '任务分组',
  `job_code` varchar(64) NOT NULL COMMENT '安全任务编码',
  `cron_expression` varchar(128) NOT NULL COMMENT 'cron表达式',
  `job_params` text COMMENT 'JSON参数',
  `allow_concurrent` tinyint NOT NULL DEFAULT 0 COMMENT '1允许并发 0串行',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0暂停',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_quartz_job_code` (`job_code`),
  KEY `idx_sys_quartz_job_status` (`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Quartz任务配置表';

CREATE TABLE IF NOT EXISTS `sys_quartz_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_id` bigint DEFAULT NULL COMMENT '任务ID',
  `job_name` varchar(100) DEFAULT NULL COMMENT '任务名称',
  `job_code` varchar(64) DEFAULT NULL COMMENT '任务编码',
  `execute_status` tinyint NOT NULL COMMENT '1成功 0失败',
  `result_summary` varchar(500) DEFAULT NULL COMMENT '执行摘要',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_quartz_job_log_job_id` (`job_id`),
  KEY `idx_sys_quartz_job_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Quartz任务执行日志';

INSERT INTO `sys_quartz_job`
(`job_name`, `job_group`, `job_code`, `cron_expression`, `job_params`, `allow_concurrent`, `status`, `remark`, `deleted`)
VALUES
('AI视频任务轮询', 'AI', 'ai_video_polling', '0/10 * * * * ?', '{}', 0, 1, '默认每10秒轮询一次AI视频生成任务', 0)
ON DUPLICATE KEY UPDATE
`job_name` = VALUES(`job_name`),
`job_group` = VALUES(`job_group`),
`cron_expression` = VALUES(`cron_expression`),
`job_params` = VALUES(`job_params`),
`allow_concurrent` = VALUES(`allow_concurrent`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`deleted` = 0;
