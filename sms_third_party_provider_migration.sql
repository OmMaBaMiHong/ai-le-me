-- 短信渠道配置迁移脚本
-- 目标：
-- 1. 将旧 sys_config 中的短信参数迁移到 sys_third_party_provider.config_json
-- 2. 统一短信配置入口到第三方服务配置页
-- 3. 当前版本只接通 aliyun 真实发送链路，tencent 仅保留配置位

SET @sms_choice := COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'chooseSms' LIMIT 1), '0');

INSERT INTO sys_third_party_provider (
    service_type, provider_code, provider_name, provider_logo,
    is_current, is_enabled, display_order, official_website, doc_url, remark, config_json,
    create_time, update_time
)
SELECT
    'sms', 'aliyun', '阿里云短信', '',
    1, 1, 1,
    'https://dysms.console.aliyun.com',
    'https://help.aliyun.com/zh/sms/',
    '短信发送当前已接入 aliyun 真实链路',
    '{}',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'sms' AND provider_code = 'aliyun'
);

INSERT INTO sys_third_party_provider (
    service_type, provider_code, provider_name, provider_logo,
    is_current, is_enabled, display_order, official_website, doc_url, remark, config_json,
    create_time, update_time
)
SELECT
    'sms', 'tencent', '腾讯云短信', '',
    0, 0, 2,
    'https://console.cloud.tencent.com/smsv2',
    'https://cloud.tencent.com/document/product/382',
    '当前只保留配置位，待后续接入真实发送通路',
    '{}',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'sms' AND provider_code = 'tencent'
);

UPDATE sys_third_party_provider
SET provider_name = '阿里云短信',
    is_current = 1,
    is_enabled = 1,
    display_order = 1,
    official_website = 'https://dysms.console.aliyun.com',
    doc_url = 'https://help.aliyun.com/zh/sms/',
    remark = '短信发送当前已接入 aliyun 真实链路',
    config_json = JSON_PRETTY(
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'region_id', JSON_OBJECT('configLabel', 'RegionId', 'helpText', '阿里云短信区域，如 cn-hangzhou', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', 'cn-hangzhou', 'displayOrder', 1, 'selectOptions', ''),
                'access_key_id', JSON_OBJECT('configLabel', 'AccessKeyId', 'helpText', '阿里云控制台 AccessKeyId', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 2, 'selectOptions', ''),
                'access_key_secret', JSON_OBJECT('configLabel', 'AccessKeySecret', 'helpText', '阿里云控制台 AccessKeySecret', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 3, 'selectOptions', ''),
                'sign_name', JSON_OBJECT('configLabel', '短信签名', 'helpText', '短信签名名称', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 4, 'selectOptions', ''),
                'template_login', JSON_OBJECT('configLabel', '登录验证码模板', 'helpText', '发送验证码时使用的短信模板编码', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 5, 'selectOptions', ''),
                'template_notify', JSON_OBJECT('configLabel', '通知模板', 'helpText', '可选，业务通知短信模板编码', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 6, 'selectOptions', '')
            ),
            'region_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_region' LIMIT 1), 'cn-hangzhou'),
            'access_key_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_access_key' LIMIT 1), ''),
            'access_key_secret', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_access_secret' LIMIT 1), ''),
            'sign_name', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_sign' LIMIT 1), ''),
            'template_login', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_templateId' LIMIT 1), ''),
            'template_notify', ''
        )
    )
WHERE service_type = 'sms' AND provider_code = 'aliyun';

UPDATE sys_third_party_provider
SET provider_name = '腾讯云短信',
    is_current = 0,
    is_enabled = 0,
    display_order = 2,
    official_website = 'https://console.cloud.tencent.com/smsv2',
    doc_url = 'https://cloud.tencent.com/document/product/382',
    remark = '当前只保留配置位，待后续接入真实发送通路',
    config_json = JSON_PRETTY(
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'secret_id', JSON_OBJECT('configLabel', 'SecretId', 'helpText', '腾讯云 API 密钥 ID', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 1, 'selectOptions', ''),
                'secret_key', JSON_OBJECT('configLabel', 'SecretKey', 'helpText', '腾讯云 API 密钥 Key', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 2, 'selectOptions', ''),
                'app_id', JSON_OBJECT('configLabel', '短信应用 ID', 'helpText', '腾讯云短信应用 SDK AppId', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 3, 'selectOptions', ''),
                'sign_name', JSON_OBJECT('configLabel', '短信签名', 'helpText', '腾讯云短信签名名称', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 4, 'selectOptions', ''),
                'template_login', JSON_OBJECT('configLabel', '登录验证码模板', 'helpText', '发送验证码时使用的模板 ID', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 5, 'selectOptions', ''),
                'template_notify', JSON_OBJECT('configLabel', '通知模板', 'helpText', '可选，业务通知模板 ID', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 6, 'selectOptions', '')
            ),
            'secret_id', '',
            'secret_key', '',
            'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsSdkAppId' LIMIT 1), ''),
            'sign_name', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsSignName' LIMIT 1), ''),
            'template_login', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsTemplateId' LIMIT 1), ''),
            'template_notify', ''
        )
    )
WHERE service_type = 'sms' AND provider_code = 'tencent';
