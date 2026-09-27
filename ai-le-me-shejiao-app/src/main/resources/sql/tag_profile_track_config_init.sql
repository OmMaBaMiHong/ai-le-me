-- 标签画像/埋点能力开关初始化（可重复执行）
-- 适用表：sys_config
-- 说明：先 UPDATE，再 INSERT...WHERE NOT EXISTS，避免依赖唯一索引

START TRANSACTION;

-- 1) 埋点总开关
UPDATE sys_config
SET
    config_name = 'App埋点总开关',
    config_value = '1',
    config_type = 'Y',
    remark = '0=关闭,1=开启；控制 /app/track/batch',
    update_time = NOW()
WHERE config_key = 'track_enabled';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time)
SELECT 'App埋点总开关', 'track_enabled', '1', 'Y', '0=关闭,1=开启；控制 /app/track/batch', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'track_enabled'
);

-- 2) AI标签建议开关
UPDATE sys_config
SET
    config_name = 'AI推荐标签开关',
    config_value = '1',
    config_type = 'Y',
    remark = '0=关闭,1=开启；控制 /app/agent/profile/tag-suggestions',
    update_time = NOW()
WHERE config_key = 'ai_tag_suggest_enabled';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time)
SELECT 'AI推荐标签开关', 'ai_tag_suggest_enabled', '1', 'Y', '0=关闭,1=开启；控制 /app/agent/profile/tag-suggestions', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'ai_tag_suggest_enabled'
);

-- 3) 标签画像聚合开关
UPDATE sys_config
SET
    config_name = '画像标签聚合开关',
    config_value = '1',
    config_type = 'Y',
    remark = '0=关闭,1=开启；控制 persona 聚合标签扩展字段',
    update_time = NOW()
WHERE config_key = 'persona_aggregate_enabled';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time)
SELECT '画像标签聚合开关', 'persona_aggregate_enabled', '1', 'Y', '0=关闭,1=开启；控制 persona 聚合标签扩展字段', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'persona_aggregate_enabled'
);

-- 4) 画像报告积分成本
UPDATE sys_config
SET
    config_name = '画像报告下载积分',
    config_value = '30',
    config_type = 'Y',
    remark = '下载画像报告默认消耗积分',
    update_time = NOW()
WHERE config_key = 'persona_report_cost_integral';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time, update_time)
SELECT '画像报告下载积分', 'persona_report_cost_integral', '30', 'Y', '下载画像报告默认消耗积分', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'persona_report_cost_integral'
);

COMMIT;

-- 校验
SELECT config_key, config_value, config_type, remark
FROM sys_config
WHERE config_key IN (
    'track_enabled',
    'ai_tag_suggest_enabled',
    'persona_aggregate_enabled',
    'persona_report_cost_integral'
)
ORDER BY config_key;
