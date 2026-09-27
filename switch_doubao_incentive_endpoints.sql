USE bang_yi;

SET @ark_api_key = COALESCE(
  (
    SELECT JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.api_key'))
    FROM sys_third_party_provider
    WHERE provider_code = 'doubao'
      AND service_type IN ('image', 'video')
      AND is_enabled = 1
      AND JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.api_key')) IS NOT NULL
      AND JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.api_key')) <> ''
    ORDER BY FIELD(service_type, 'image', 'video'), provider_id
    LIMIT 1
  ),
  ''
);

SET @ai_doubao_config = JSON_PRETTY(JSON_OBJECT(
  '_meta', JSON_OBJECT(
    'endpoint', JSON_OBJECT('configLabel', 'API Endpoint', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', 'https://ark.cn-beijing.volces.com/api/v3', 'displayOrder', 1, 'helpText', 'Doubao Ark base endpoint'),
    'api_key', JSON_OBJECT('configLabel', 'API Key', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 2, 'helpText', 'ARK API key'),
    'model', JSON_OBJECT('configLabel', 'Model', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', 'ep-20260323145008-tbqqd', 'displayOrder', 3, 'helpText', 'Doubao-Seed-2.0-pro incentive endpoint'),
    'api_protocol', JSON_OBJECT('configLabel', 'API Protocol', 'valueType', 'select', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', 'responses', 'displayOrder', 4, 'helpText', 'responses or chat_completions', 'selectOptions', '["responses","chat_completions"]'),
    'timeout', JSON_OBJECT('configLabel', 'Timeout(s)', 'valueType', 'number', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '30', 'displayOrder', 5, 'helpText', 'Request timeout seconds')
  ),
  'endpoint', 'https://ark.cn-beijing.volces.com/api/v3',
  'api_key', @ark_api_key,
  'model', 'ep-20260323145008-tbqqd',
  'api_protocol', 'responses',
  'timeout', '30',
  'defaultProfile', 'doubao_default',
  'profiles', JSON_ARRAY(
    JSON_OBJECT('code', 'doubao_default', 'model', 'ep-20260323145008-tbqqd', 'api_protocol', 'responses'),
    JSON_OBJECT('code', 'agent_content_planner', 'model', 'ep-20260323145008-tbqqd', 'api_protocol', 'responses'),
    JSON_OBJECT('code', 'agent_content_copy', 'model', 'ep-20260323145008-tbqqd', 'api_protocol', 'responses')
  )
));

INSERT INTO sys_third_party_provider (
  service_type,
  provider_code,
  provider_name,
  provider_logo,
  is_current,
  is_enabled,
  display_order,
  official_website,
  doc_url,
  remark,
  config_json,
  create_time,
  update_time
)
VALUES (
  'ai',
  'doubao',
  'Doubao Ark',
  NULL,
  1,
  1,
  1,
  'https://www.volcengine.com/product/ark',
  'https://www.volcengine.com/docs/82379/1569618',
  'Default AI routing switched to Doubao incentive endpoint',
  @ai_doubao_config,
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  is_enabled = 1,
  display_order = VALUES(display_order),
  official_website = VALUES(official_website),
  doc_url = VALUES(doc_url),
  remark = VALUES(remark),
  config_json = VALUES(config_json),
  update_time = NOW();

UPDATE sys_third_party_provider
SET is_current = CASE WHEN provider_code = 'doubao' THEN 1 ELSE 0 END,
    update_time = NOW()
WHERE service_type = 'ai'
  AND provider_code <> 'common';

UPDATE sys_third_party_route_rule
SET provider_code = 'doubao',
    profile_code = 'agent_content_copy',
    update_time = NOW()
WHERE service_type = 'ai'
  AND scene_code IN ('agent_content_image_post', 'agent_content_ai_video');

DELETE FROM sys_third_party_route_rule
WHERE service_type = 'ai'
  AND scene_code = 'agent_content_planner';

INSERT INTO sys_third_party_route_rule (
  service_type,
  scene_code,
  function_type,
  provider_code,
  profile_code,
  priority,
  is_enabled,
  remark,
  create_time,
  update_time
)
VALUES (
  'ai',
  'agent_content_planner',
  'agent_content_planner',
  'doubao',
  'agent_content_planner',
  5,
  1,
  'Agent content planner uses Doubao Seed 2.0 Pro incentive endpoint',
  NOW(),
  NOW()
);

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
  JSON_SET(
    JSON_SET(
      JSON_SET(
        JSON_SET(CAST(config_json AS JSON), '$.model', 'ep-20260318121955-mjq65'),
        '$.defaultProfile', 'seedance_v15_default'
      ),
      '$.profiles[0].model', 'ep-20260318121955-mjq65'
    ),
    '$.profiles[1].model', 'ep-20260318121955-mjq65'
  )
),
update_time = NOW()
WHERE service_type = 'video'
  AND provider_code = 'doubao';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
  JSON_SET(
    JSON_SET(
      JSON_SET(
        JSON_SET(
          CAST(config_json AS JSON),
          '$.profiles[0].model', 'ep-20260323151528-8khnp'
        ),
        '$.profiles[0].fallback_model', 'doubao-seedream-4-5-251128'
      ),
      '$.profiles[0].fallback_models', 'doubao-seedream-4-5-251128,doubao-seedream-4-0-250828,doubao-seedream-3-0-t2i-250415'
    ),
    '$.profiles[1].model', 'ep-20260323151528-8khnp'
  )
),
update_time = NOW()
WHERE service_type = 'image'
  AND provider_code = 'doubao';
