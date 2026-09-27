SET @match_case_source_snapshot_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'hongniang_match_case'
    AND column_name = 'source_snapshot_json'
);
SET @add_match_case_source_snapshot_sql := IF(
  @match_case_source_snapshot_exists = 0,
  'ALTER TABLE `hongniang_match_case` ADD COLUMN `source_snapshot_json` text NULL COMMENT ''来源快照'' AFTER `source_ref_id`',
  'SELECT 1'
);
PREPARE stmt FROM @add_match_case_source_snapshot_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @match_case_latest_request_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'hongniang_match_case'
    AND column_name = 'latest_request_status'
);
SET @add_match_case_latest_request_sql := IF(
  @match_case_latest_request_exists = 0,
  'ALTER TABLE `hongniang_match_case` ADD COLUMN `latest_request_status` tinyint NULL COMMENT ''最近牵线申请状态'' AFTER `source_snapshot_json`',
  'SELECT 1'
);
PREPARE stmt FROM @add_match_case_latest_request_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `hongniang_match_request` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `case_id` int NOT NULL COMMENT '案件ID',
  `hongniang_id` int NOT NULL COMMENT '红娘ID',
  `from_user_id` int NOT NULL COMMENT '发起用户ID',
  `to_user_id` int NOT NULL COMMENT '接收用户ID',
  `request_channel` tinyint NOT NULL DEFAULT 1 COMMENT '申请渠道：1App私信申请 2红娘代分享微信',
  `request_status` tinyint NOT NULL DEFAULT 0 COMMENT '申请状态：0待发送 1已发送 2已接受 3已拒绝 4已过期',
  `request_message` varchar(500) DEFAULT NULL COMMENT '申请文案',
  `intent_request_id` varchar(64) DEFAULT NULL COMMENT '意图消息请求ID',
  `wechat_share_snapshot` varchar(255) DEFAULT NULL COMMENT '代分享微信快照',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_match_request_intent` (`intent_request_id`),
  KEY `idx_case_status` (`case_id`, `request_status`),
  KEY `idx_to_user_status` (`to_user_id`, `request_status`),
  KEY `idx_case_from_to` (`case_id`, `from_user_id`, `to_user_id`, `request_channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红娘牵线申请';

CREATE TABLE IF NOT EXISTS `hongniang_group_sop_template` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `hongniang_id` int NOT NULL COMMENT '红娘ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_type` tinyint NOT NULL DEFAULT 1 COMMENT '模板类型：1欢迎语 2群规则 3活动召回 4跟进提醒',
  `content_type` tinyint NOT NULL DEFAULT 1 COMMENT '内容类型：1文本 2图文 3链接',
  `content_payload` text COMMENT '内容载荷',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_hongniang_template` (`hongniang_id`, `template_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红娘群SOP模板';

CREATE TABLE IF NOT EXISTS `hongniang_group_task_execution` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '执行ID',
  `task_id` int NOT NULL COMMENT '任务ID',
  `group_id` int NOT NULL COMMENT '群ID',
  `provider_type` tinyint NOT NULL DEFAULT 0 COMMENT '接入类型',
  `execution_status` tinyint NOT NULL DEFAULT 0 COMMENT '执行状态：0待执行 1执行中 2成功 3失败 4仅登记',
  `request_payload` text COMMENT '请求载荷',
  `provider_task_id` varchar(128) DEFAULT NULL COMMENT '三方任务ID',
  `provider_response` text COMMENT '三方响应',
  `result_summary` varchar(1000) DEFAULT NULL COMMENT '结果摘要',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_execution` (`task_id`, `execution_status`, `create_time`),
  KEY `idx_group_execution` (`group_id`, `execution_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红娘群任务执行记录';

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
  '红娘私域总览', @hongniang_parent_menu_id, 20, 'private-domain-dashboard', 'shejiao/hongniang/private-domain-dashboard/index', 1, 0,
  'C', '0', '0', 'hongniang:privateDomain:list', 'dashboard', 1, NOW(), 1, NOW(), '红娘私域经营总览'
FROM dual
WHERE @hongniang_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `parent_id` = @hongniang_parent_menu_id
      AND `component` = 'shejiao/hongniang/private-domain-dashboard/index'
  );

SET @private_domain_menu_id := (
  SELECT `menu_id`
  FROM `sys_menu`
  WHERE `parent_id` = @hongniang_parent_menu_id
    AND `component` = 'shejiao/hongniang/private-domain-dashboard/index'
  ORDER BY `menu_id`
  LIMIT 1
);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `menu_type`, `visible`, `status`, `perms`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT '私域总览查询', @private_domain_menu_id, 1, 'F', '0', '0', 'hongniang:privateDomain:query', 1, NOW(), 1, NOW()
FROM dual
WHERE @private_domain_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @private_domain_menu_id AND `perms` = 'hongniang:privateDomain:query');

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'wecom_customer', 'common', '企业微信客户群', 1, 1, 1,
       'https://work.weixin.qq.com',
       'https://developer.work.weixin.qq.com/document/path/90665',
       '企业微信客户群同步与触达配置',
       JSON_OBJECT(),
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'wecom_customer' AND provider_code = 'common'
);

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'scrm_vendor', 'common', 'SCRM Bridge', 1, 1, 1,
       '',
       '',
       'SCRM HTTP Bridge 配置',
       JSON_OBJECT(),
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'scrm_vendor' AND provider_code = 'common'
);

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'corpId', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.corpId')), ''),
            'corpSecret', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.corpSecret')), ''),
            'agentId', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.agentId')), ''),
            'token', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.token')), ''),
            'aesKey', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.aesKey')), ''),
            '_meta', JSON_OBJECT(
                'corpId', JSON_OBJECT('configLabel', 'CorpId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', '企业微信 CorpId'),
                'corpSecret', JSON_OBJECT('configLabel', 'CorpSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 2, 'helpText', '企业微信应用 Secret'),
                'agentId', JSON_OBJECT('configLabel', 'AgentId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 3, 'helpText', '企业微信应用 AgentId'),
                'token', JSON_OBJECT('configLabel', 'Token', 'defaultValue', '', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 4, 'helpText', '企微回调 Token'),
                'aesKey', JSON_OBJECT('configLabel', 'AESKey', 'defaultValue', '', 'valueType', 'password', 'isRequired', 0, 'isSensitive', 1, 'displayOrder', 5, 'helpText', '企微回调 AESKey')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'wecom_customer' AND provider_code = 'common';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'baseUrl', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.baseUrl')), ''),
            'appKey', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.appKey')), ''),
            'appSecret', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.appSecret')), ''),
            'vendorCode', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.vendorCode')), ''),
            '_meta', JSON_OBJECT(
                'baseUrl', JSON_OBJECT('configLabel', '基础地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', 'SCRM HTTP Bridge 根地址'),
                'appKey', JSON_OBJECT('configLabel', 'AppKey', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 2, 'helpText', 'SCRM 接入 AppKey'),
                'appSecret', JSON_OBJECT('configLabel', 'AppSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 3, 'helpText', 'SCRM 接入 AppSecret'),
                'vendorCode', JSON_OBJECT('configLabel', 'VendorCode', 'defaultValue', '', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 4, 'helpText', 'SCRM 厂商编码')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'scrm_vendor' AND provider_code = 'common';
