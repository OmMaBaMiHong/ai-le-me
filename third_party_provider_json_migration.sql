USE bang_yi;

-- 1. 为旧配置中存在但 provider 表尚未存在的渠道补齐 provider 记录
INSERT INTO sys_third_party_provider (
    service_type,
    provider_code,
    provider_name,
    is_current,
    is_enabled,
    display_order,
    remark,
    config_json,
    create_time,
    update_time
)
SELECT
    c.service_type,
    c.provider,
    CASE
        WHEN c.provider = 'common' THEN '共享配置'
        ELSE UPPER(c.provider)
    END AS provider_name,
    0 AS is_current,
    1 AS is_enabled,
    999 AS display_order,
    '由 sys_third_party_config 自动迁移生成' AS remark,
    '{}' AS config_json,
    NOW(),
    NOW()
FROM (
    SELECT DISTINCT service_type, provider
    FROM sys_third_party_config
) c
LEFT JOIN sys_third_party_provider p
    ON p.service_type = c.service_type
   AND p.provider_code = c.provider
WHERE p.provider_id IS NULL;

-- 2. 将旧表 key-value 与展示元数据合并进 provider.config_json
UPDATE sys_third_party_provider p
JOIN (
    SELECT
        service_type,
        provider,
        JSON_OBJECTAGG(
            config_key,
            COALESCE(NULLIF(config_value, ''), default_value, '')
        ) AS value_json,
        JSON_OBJECTAGG(
            config_key,
            JSON_OBJECT(
                'configLabel', COALESCE(config_label, config_key),
                'defaultValue', COALESCE(default_value, ''),
                'valueType', COALESCE(value_type, 'text'),
                'selectOptions', COALESCE(select_options, ''),
                'isRequired', COALESCE(is_required, 0),
                'isSensitive', COALESCE(is_sensitive, 0),
                'displayOrder', COALESCE(display_order, 0),
                'helpText', COALESCE(help_text, '')
            )
        ) AS meta_json
    FROM sys_third_party_config
    GROUP BY service_type, provider
) agg
  ON agg.service_type = p.service_type
 AND agg.provider = p.provider_code
SET p.config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(p.config_json), JSON_EXTRACT(p.config_json, '$'), JSON_OBJECT()),
        agg.value_json,
        JSON_OBJECT('_meta', agg.meta_json)
    )
);

-- 3. 删除旧配置表
DROP TABLE IF EXISTS sys_third_party_config;
