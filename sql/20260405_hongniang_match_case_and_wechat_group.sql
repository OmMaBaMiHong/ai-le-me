CREATE TABLE IF NOT EXISTS `hongniang_match_case` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '案件ID',
  `hongniang_id` int NOT NULL COMMENT '红娘ID',
  `male_user_id` int NOT NULL COMMENT '男方用户ID',
  `female_user_id` int NOT NULL COMMENT '女方用户ID',
  `current_stage` tinyint NOT NULL DEFAULT 0 COMMENT '当前阶段：0待建档 1推荐中 2已建联 3已见面 4交往中 5见家长 6已结婚 7已生子 8已关闭',
  `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '来源类型：1手工建档 2智能推荐快照 3相亲活动 4微信群组局',
  `source_ref_id` bigint DEFAULT NULL COMMENT '来源关联ID',
  `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
  `last_follow_time` datetime DEFAULT NULL COMMENT '最近跟进时间',
  `close_reason` varchar(500) DEFAULT NULL COMMENT '关闭原因',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hongniang_stage` (`hongniang_id`, `current_stage`),
  KEY `idx_hongniang_pair_open` (`hongniang_id`, `male_user_id`, `female_user_id`, `current_stage`),
  KEY `idx_next_follow_time` (`next_follow_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红娘牵线案件';

CREATE TABLE IF NOT EXISTS `hongniang_match_progress` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '进度ID',
  `case_id` int NOT NULL COMMENT '案件ID',
  `progress_type` tinyint NOT NULL COMMENT '进度类型：1建档 2阶段推进 3跟进记录 4关闭案件',
  `stage_before` tinyint DEFAULT NULL COMMENT '变更前阶段',
  `stage_after` tinyint DEFAULT NULL COMMENT '变更后阶段',
  `content` varchar(2000) DEFAULT NULL COMMENT '进度内容',
  `planned_follow_time` datetime DEFAULT NULL COMMENT '计划跟进时间',
  `actual_follow_time` datetime DEFAULT NULL COMMENT '实际跟进时间',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `attachments` varchar(2000) DEFAULT NULL COMMENT '附件JSON',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_case_create_time` (`case_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红娘牵线案件进度';

CREATE TABLE IF NOT EXISTS `hongniang_match_case_group` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `case_id` int NOT NULL COMMENT '案件ID',
  `group_id` int NOT NULL COMMENT '微信群ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_case_group` (`case_id`, `group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='牵线案件微信群关联';

CREATE TABLE IF NOT EXISTS `hongniang_wechat_group` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '群ID',
  `hongniang_id` int NOT NULL COMMENT '红娘ID',
  `group_name` varchar(100) NOT NULL COMMENT '群名称',
  `group_type` tinyint NOT NULL DEFAULT 1 COMMENT '群类型：1现有微信群 2企微客户群',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '群主名称',
  `owner_wechat` varchar(100) DEFAULT NULL COMMENT '群主微信',
  `tag_json` varchar(2000) DEFAULT NULL COMMENT '标签JSON',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `purpose` varchar(255) DEFAULT NULL COMMENT '用途',
  `qr_code_url` varchar(500) DEFAULT NULL COMMENT '二维码地址',
  `join_link` varchar(500) DEFAULT NULL COMMENT '加群链接',
  `external_group_id` varchar(128) DEFAULT NULL COMMENT '外部群ID',
  `provider_type` tinyint NOT NULL DEFAULT 0 COMMENT '接入类型：0手工 1企业微信 2SCRM',
  `sync_status` tinyint NOT NULL DEFAULT 0 COMMENT '同步状态：0未同步 1已同步 2同步失败',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
  `remark` varchar(1000) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hongniang_group` (`hongniang_id`, `group_type`, `sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红娘微信群资产';

CREATE TABLE IF NOT EXISTS `hongniang_wechat_group_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `group_id` int NOT NULL COMMENT '群ID',
  `user_id` int NOT NULL COMMENT '平台用户ID',
  `relation_id` int DEFAULT NULL COMMENT '红娘用户归属关系ID',
  `join_time` datetime DEFAULT NULL COMMENT '入群时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信群绑定平台用户';

CREATE TABLE IF NOT EXISTS `hongniang_group_touch_task` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `hongniang_id` int NOT NULL COMMENT '红娘ID',
  `group_id` int NOT NULL COMMENT '群ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `send_scope_type` tinyint NOT NULL DEFAULT 1 COMMENT '发送范围：1全群 2标签 3案件相关 4指定用户',
  `content_type` tinyint NOT NULL DEFAULT 1 COMMENT '内容类型：1文本 2图文 3链接',
  `content_payload` text COMMENT '内容载荷JSON',
  `schedule_time` datetime DEFAULT NULL COMMENT '计划发送时间',
  `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态：0待发送 1发送中 2已完成 3失败 4仅登记',
  `provider_type` tinyint NOT NULL DEFAULT 0 COMMENT '接入类型：0手工 1企业微信 2SCRM',
  `provider_task_id` varchar(128) DEFAULT NULL COMMENT '三方任务ID',
  `result_summary` varchar(1000) DEFAULT NULL COMMENT '结果摘要',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_status` (`group_id`, `task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信群触达任务';

CREATE TABLE IF NOT EXISTS `hongniang_group_touch_log` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `task_id` int NOT NULL COMMENT '任务ID',
  `group_id` int NOT NULL COMMENT '群ID',
  `send_status` tinyint NOT NULL DEFAULT 0 COMMENT '发送状态：0待处理 1成功 2失败 3仅登记',
  `provider_response` text COMMENT '三方响应',
  `sent_time` datetime DEFAULT NULL COMMENT '发送时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_log` (`task_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信群触达日志';

SET @match_case_uk_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'hongniang_match_case'
    AND index_name = 'uk_hongniang_pair_open'
);
SET @drop_match_case_uk_sql := IF(@match_case_uk_exists > 0,
  'ALTER TABLE `hongniang_match_case` DROP INDEX `uk_hongniang_pair_open`',
  'SELECT 1');
PREPARE stmt FROM @drop_match_case_uk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @match_case_idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'hongniang_match_case'
    AND index_name = 'idx_hongniang_pair_open'
);
SET @add_match_case_idx_sql := IF(@match_case_idx_exists = 0,
  'ALTER TABLE `hongniang_match_case` ADD INDEX `idx_hongniang_pair_open` (`hongniang_id`, `male_user_id`, `female_user_id`, `current_stage`)',
  'SELECT 1');
PREPARE stmt FROM @add_match_case_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_menu`
SET `menu_name` = '用户池管理',
    `perms` = IFNULL(`perms`, 'hongniang:hongniangUser:list'),
    `update_by` = 1,
    `update_time` = NOW()
WHERE `component` = 'shejiao/hongniang/hongniang-user/index'
   OR (`menu_name` = '单身贵族' AND `path` = 'hongniang-user');

SET @hongniang_user_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `component` = 'shejiao/hongniang/hongniang-user/index'
     OR `path` = 'hongniang-user'
  ORDER BY `menu_id`
  LIMIT 1
);

SET @hongniang_parent_menu_id := (
  SELECT `parent_id`
  FROM `sys_menu`
  WHERE `menu_id` = @hongniang_user_menu_id
  LIMIT 1
);

SET @hongniang_parent_menu_id := IFNULL(
  @hongniang_parent_menu_id,
  (
    SELECT `menu_id`
    FROM `sys_menu`
    WHERE `menu_name` = '红娘管理'
    ORDER BY `menu_id`
    LIMIT 1
  )
);

INSERT INTO `sys_menu` (
  `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
  `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
)
SELECT
  '牵线案件', @hongniang_parent_menu_id, 21, 'match-case', 'shejiao/hongniang/match-case/index', 1, 0,
  'C', '0', '0', 'hongniang:matchCase:list', 'guide', 1, NOW(), 1, NOW(), '红娘牵线案件管理'
FROM dual
WHERE @hongniang_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hongniang_parent_menu_id
      AND `component` = 'shejiao/hongniang/match-case/index'
  );

SET @match_case_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = @hongniang_parent_menu_id
    AND `component` = 'shejiao/hongniang/match-case/index'
  ORDER BY `menu_id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '牵线案件查询', @match_case_menu_id, 1, 'F', '0', '0', 'hongniang:matchCase:query', 1, NOW(), 1, NOW()
FROM dual
WHERE @match_case_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @match_case_menu_id AND `perms` = 'hongniang:matchCase:query');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '牵线案件新增', @match_case_menu_id, 2, 'F', '0', '0', 'hongniang:matchCase:add', 1, NOW(), 1, NOW()
FROM dual
WHERE @match_case_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @match_case_menu_id AND `perms` = 'hongniang:matchCase:add');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '牵线案件修改', @match_case_menu_id, 3, 'F', '0', '0', 'hongniang:matchCase:edit', 1, NOW(), 1, NOW()
FROM dual
WHERE @match_case_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @match_case_menu_id AND `perms` = 'hongniang:matchCase:edit');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '牵线案件关闭', @match_case_menu_id, 4, 'F', '0', '0', 'hongniang:matchCase:close', 1, NOW(), 1, NOW()
FROM dual
WHERE @match_case_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @match_case_menu_id AND `perms` = 'hongniang:matchCase:close');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '牵线案件绑群', @match_case_menu_id, 5, 'F', '0', '0', 'hongniang:matchCase:bindGroup', 1, NOW(), 1, NOW()
FROM dual
WHERE @match_case_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @match_case_menu_id AND `perms` = 'hongniang:matchCase:bindGroup');

INSERT INTO `sys_menu` (
  `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
  `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`
)
SELECT
  '微信群管理', @hongniang_parent_menu_id, 22, 'wechat-group', 'shejiao/hongniang/wechat-group/index', 1, 0,
  'C', '0', '0', 'hongniang:wechatGroup:list', 'peoples', 1, NOW(), 1, NOW(), '红娘微信群资产管理'
FROM dual
WHERE @hongniang_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hongniang_parent_menu_id
      AND `component` = 'shejiao/hongniang/wechat-group/index'
  );

SET @wechat_group_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = @hongniang_parent_menu_id
    AND `component` = 'shejiao/hongniang/wechat-group/index'
  ORDER BY `menu_id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '微信群查询', @wechat_group_menu_id, 1, 'F', '0', '0', 'hongniang:wechatGroup:query', 1, NOW(), 1, NOW()
FROM dual
WHERE @wechat_group_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @wechat_group_menu_id AND `perms` = 'hongniang:wechatGroup:query');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '微信群新增', @wechat_group_menu_id, 2, 'F', '0', '0', 'hongniang:wechatGroup:add', 1, NOW(), 1, NOW()
FROM dual
WHERE @wechat_group_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @wechat_group_menu_id AND `perms` = 'hongniang:wechatGroup:add');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '微信群修改', @wechat_group_menu_id, 3, 'F', '0', '0', 'hongniang:wechatGroup:edit', 1, NOW(), 1, NOW()
FROM dual
WHERE @wechat_group_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @wechat_group_menu_id AND `perms` = 'hongniang:wechatGroup:edit');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '微信群同步', @wechat_group_menu_id, 4, 'F', '0', '0', 'hongniang:wechatGroup:sync', 1, NOW(), 1, NOW()
FROM dual
WHERE @wechat_group_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @wechat_group_menu_id AND `perms` = 'hongniang:wechatGroup:sync');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '群触达任务管理', @wechat_group_menu_id, 5, 'F', '0', '0', 'hongniang:groupTouch:edit', 1, NOW(), 1, NOW()
FROM dual
WHERE @wechat_group_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @wechat_group_menu_id AND `perms` = 'hongniang:groupTouch:edit');
