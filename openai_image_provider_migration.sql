USE bang_yi;

SET @openai_image_config = JSON_PRETTY(JSON_OBJECT(
  '_meta', JSON_OBJECT(
    'endpoint', JSON_OBJECT('configLabel', 'API Endpoint', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', 'https://api.openai.com/v1', 'displayOrder', 1, 'helpText', 'OpenAI API base endpoint'),
    'api_key', JSON_OBJECT('configLabel', 'API Key', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 2, 'helpText', 'OpenAI API key'),
    'model', JSON_OBJECT('configLabel', 'Responses Model', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', 'gpt-4.1', 'displayOrder', 3, 'helpText', 'Mainline model used to call the image_generation tool'),
    'size', JSON_OBJECT('configLabel', 'Default Size', 'valueType', 'select', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '1024x1536', 'displayOrder', 4, 'helpText', 'Portrait is recommended for dating profile content', 'selectOptions', '["1024x1024","1024x1536","1536x1024"]'),
    'quality', JSON_OBJECT('configLabel', 'Quality', 'valueType', 'select', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', 'high', 'displayOrder', 5, 'helpText', 'Image quality preference', 'selectOptions', '["auto","low","medium","high"]'),
    'background', JSON_OBJECT('configLabel', 'Background', 'valueType', 'select', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', 'auto', 'displayOrder', 6, 'helpText', 'Transparent background is only suitable for cutout material', 'selectOptions', '["auto","opaque","transparent"]'),
    'input_fidelity', JSON_OBJECT('configLabel', 'Input Fidelity', 'valueType', 'select', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', 'high', 'displayOrder', 7, 'helpText', 'Use high when preserving the same face/identity matters', 'selectOptions', '["low","high"]'),
    'max_images', JSON_OBJECT('configLabel', 'Max Images', 'valueType', 'number', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '3', 'displayOrder', 8, 'helpText', 'Provider-side upper bound per request'),
    'max_reference_images', JSON_OBJECT('configLabel', 'Max Reference Images', 'valueType', 'number', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '4', 'displayOrder', 9, 'helpText', 'How many avatar/figure URLs to pass into one edit request'),
    'timeout', JSON_OBJECT('configLabel', 'Timeout(s)', 'valueType', 'number', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '120', 'displayOrder', 10, 'helpText', 'Request timeout seconds'),
    'organization_id', JSON_OBJECT('configLabel', 'Organization ID', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 11, 'helpText', 'Optional OpenAI organization header'),
    'project_id', JSON_OBJECT('configLabel', 'Project ID', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 12, 'helpText', 'Optional OpenAI project header')
  ),
  'endpoint', 'https://api.openai.com/v1',
  'api_key', '',
  'model', 'gpt-4.1',
  'size', '1024x1536',
  'quality', 'high',
  'background', 'auto',
  'input_fidelity', 'high',
  'max_images', '3',
  'max_reference_images', '4',
  'timeout', '120',
  'organization_id', '',
  'project_id', ''
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
  'image',
  'openai',
  'OpenAI GPT Image',
  NULL,
  0,
  1,
  2,
  'https://openai.com',
  'https://platform.openai.com/docs/guides/images/image-generation',
  'OpenAI image generation provider using Responses API image_generation tool and OSS upload',
  @openai_image_config,
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  provider_name = VALUES(provider_name),
  is_enabled = VALUES(is_enabled),
  display_order = VALUES(display_order),
  official_website = VALUES(official_website),
  doc_url = VALUES(doc_url),
  remark = VALUES(remark),
  config_json = VALUES(config_json),
  update_time = NOW();
