SET NAMES utf8mb4;
USE bang_yi;

START TRANSACTION;

-- 1. 修复 third provider 中文乱码
UPDATE sys_third_party_provider
SET provider_name = CASE
        WHEN service_type = 'ai' AND provider_code = 'mock' THEN '默认 Mock'
        WHEN service_type = 'ai' AND provider_code = 'tongyi' THEN '通义千问'
        WHEN service_type = 'ai' AND provider_code = 'zhipu' THEN '智谱 GLM'
        WHEN service_type = 'education' AND provider_code = 'xuexin' THEN '学信网'
        WHEN service_type = 'map' AND provider_code = 'tencent' THEN '腾讯地图'
        WHEN service_type = 'map' AND provider_code = 'amap' THEN '高德地图'
        WHEN service_type = 'map' AND provider_code = 'baidu' THEN '百度地图'
        WHEN service_type = 'oss' AND provider_code = 'tencent' THEN '腾讯云 COS'
        WHEN service_type = 'oss' AND provider_code = 'aliyun' THEN '阿里云 OSS'
        WHEN service_type = 'oss' AND provider_code = 'qiniu' THEN '七牛云'
        WHEN service_type = 'payment' AND provider_code = 'wechat' THEN '微信支付'
        WHEN service_type = 'payment' AND provider_code = 'alipay' THEN '支付宝'
        WHEN service_type = 'push' AND provider_code = 'jpush' THEN '极光推送'
        WHEN service_type = 'push' AND provider_code = 'umeng' THEN '友盟推送'
        WHEN service_type = 'realname' AND provider_code = 'tencent' THEN '腾讯云实名认证'
        WHEN service_type = 'realname' AND provider_code = 'aliyun' THEN '阿里云实名认证'
        WHEN service_type = 'video' AND provider_code = 'jimeng' THEN '即梦'
        WHEN service_type = 'video' AND provider_code = 'kling' THEN '可灵'
        ELSE provider_name
    END
WHERE (service_type, provider_code) IN (
    ('ai', 'mock'),
    ('ai', 'tongyi'),
    ('ai', 'zhipu'),
    ('education', 'xuexin'),
    ('map', 'tencent'),
    ('map', 'amap'),
    ('map', 'baidu'),
    ('oss', 'tencent'),
    ('oss', 'aliyun'),
    ('oss', 'qiniu'),
    ('payment', 'wechat'),
    ('payment', 'alipay'),
    ('push', 'jpush'),
    ('push', 'umeng'),
    ('realname', 'tencent'),
    ('realname', 'aliyun'),
    ('video', 'jimeng'),
    ('video', 'kling')
);

UPDATE sys_third_party_provider
SET provider_name = CONVERT(BINARY(CONVERT(provider_name USING latin1)) USING utf8mb4)
WHERE provider_id IN (7, 8, 28, 29, 30);

UPDATE sys_third_party_provider
SET config_json = CONVERT(BINARY(CONVERT(config_json USING latin1)) USING utf8mb4)
WHERE provider_id IN (4, 5, 9, 10);

-- 2. 收敛短信配置
UPDATE sys_third_party_provider
SET is_current = CASE
        WHEN COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'chooseSms' LIMIT 1), '0') = '1' THEN 0
        ELSE 1
    END,
    is_enabled = 1,
    config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'region_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_region' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.region_id')), 'cn-hangzhou'),
                'access_key_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_access_key' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.access_key_id')), ''),
                'access_key_secret', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_access_secret' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.access_key_secret')), ''),
                'sign_name', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_sign' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.sign_name')), ''),
                'template_login', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'sms_templateconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.template_login')), ''),
                'template_notify', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.template_notify')), '')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'sms' AND provider_code = 'aliyun';

UPDATE sys_third_party_provider
SET is_current = CASE
        WHEN COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'chooseSms' LIMIT 1), '0') = '1' THEN 1
        ELSE 0
    END,
    is_enabled = 1,
    config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'region', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsRegion' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.region')), 'ap-beijing'),
                'secret_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSecretconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.secret_id')), ''),
                'secret_key', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSecretKey' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.secret_key')), ''),
                'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsSdkAppconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_id')), ''),
                'sign_name', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsSignName' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.sign_name')), ''),
                'template_login', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'tencentSmsTemplateconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.template_login')), ''),
                'template_notify', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.template_notify')), '')
            ),
            JSON_OBJECT(
                '_meta', JSON_MERGE_PATCH(
                    COALESCE(JSON_EXTRACT(config_json, '$._meta'), JSON_OBJECT()),
                    JSON_OBJECT(
                        'region', JSON_OBJECT('configLabel', 'Region', 'helpText', '腾讯云短信地域，如 ap-beijing', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', 'ap-beijing', 'displayOrder', 6, 'selectOptions', '')
                    )
                )
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'sms' AND provider_code = 'tencent';

-- 3. 收敛微信与支付配置
UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'WxAppId' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_id')), ''),
                'app_secret', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxAppSecret' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_secret')), ''),
                'ad_pid', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxAdpconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.ad_pid')), '')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'wechat_mini' AND provider_code = 'wechat';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'app_id', COALESCE(
                    (SELECT config_value FROM sys_config WHERE config_key = 'WxMpAppId' LIMIT 1),
                    (SELECT config_value FROM sys_config WHERE config_key = 'WxMpAppconfig_id' LIMIT 1),
                    JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_id')),
                    ''
                ),
                'app_secret', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'WxMpSecret' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_secret')), '')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'wechat_mp' AND provider_code = 'wechat';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'app_appconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_id')), '')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'wechat_app' AND provider_code = 'wechat';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'WxMpAppId' LIMIT 1), (SELECT config_value FROM sys_config WHERE config_key = 'WxMpAppconfig_id' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.app_id')), ''),
                'mch_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxPayKey' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.mch_id')), ''),
                'api_key', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxPaySecret' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.api_key')), ''),
                'notify_url', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'appNotifyurl' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.notify_url')), ''),
                'h5_redirect_url', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'redirectUrl' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.h5_redirect_url')), ''),
                'h5_domain', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.h5_domain')), '')
            ),
            JSON_OBJECT(
                '_meta', JSON_MERGE_PATCH(
                    COALESCE(JSON_EXTRACT(config_json, '$._meta'), JSON_OBJECT()),
                    JSON_OBJECT(
                        'h5_redirect_url', JSON_OBJECT('configLabel', 'H5跳转地址', 'helpText', '微信 H5 支付完成后的回跳地址', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 8, 'selectOptions', '')
                    )
                )
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'payment' AND provider_code = 'wechat';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'return_url', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'redirectUrl' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.return_url')), '')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'payment' AND provider_code = 'alipay';

-- 4. 收敛 OSS 配置，优先沿用旧 oss.provider；未配置时默认七牛
UPDATE sys_third_party_provider
SET is_current = CASE
        WHEN provider_code = COALESCE(
            (SELECT CASE
                        WHEN config_value = 'qcloud' THEN 'tencent'
                        ELSE config_value
                    END
             FROM sys_config
             WHERE config_key = 'oss.provider'
             LIMIT 1),
            'qiniu'
        ) THEN 1
        ELSE 0
    END,
    is_enabled = 1,
    update_time = NOW()
WHERE service_type = 'oss';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'domain', JSON_OBJECT('configLabel', '访问域名', 'helpText', '七牛绑定的访问域名', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 1, 'selectOptions', ''),
                'prefix', JSON_OBJECT('configLabel', '目录前缀', 'helpText', '上传路径前缀，可为空', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 2, 'selectOptions', ''),
                'access_key', JSON_OBJECT('configLabel', 'AccessKey', 'helpText', '七牛 AccessKey', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 3, 'selectOptions', ''),
                'secret_key', JSON_OBJECT('configLabel', 'SecretKey', 'helpText', '七牛 SecretKey', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'defaultValue', '', 'displayOrder', 4, 'selectOptions', ''),
                'bucket_name', JSON_OBJECT('configLabel', 'Bucket', 'helpText', '七牛空间名称', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'defaultValue', '', 'displayOrder', 5, 'selectOptions', '')
            ),
            'domain', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.qiniuDomain')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.domain')), ''),
            'prefix', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.qiniuPrefix')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.prefix')), ''),
            'access_key', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.qiniuAccessKey')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.access_key')), ''),
            'secret_key', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.qiniuSecretKey')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.secret_key')), ''),
            'bucket_name', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.qiniuBucketName')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.bucket_name')), '')
        )
    ),
    update_time = NOW()
WHERE service_type = 'oss' AND provider_code = 'qiniu';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'domain', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.aliyunDomain')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.domain')), ''),
                'prefix', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.aliyunPrefix')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.prefix')), ''),
                'endpoint', REPLACE(COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.aliyunEndPoint')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.endpoint')), ''), 'https://', ''),
                'access_key_id', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.aliyunAccessKeyconfig_id')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.access_key_id')), ''),
                'access_key_secret', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.aliyunAccessKeySecret')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.access_key_secret')), ''),
                'bucket', COALESCE(JSON_UNQUOTE(JSON_EXTRACT((SELECT config_value FROM sys_config WHERE config_key = 'CLOUD_STORAGE_CONFIG_KEY' LIMIT 1), '$.aliyunBucketName')), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.bucket')), '')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'oss' AND provider_code = 'aliyun';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
        JSON_MERGE_PATCH(
            IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
            JSON_OBJECT(
                'domain', COALESCE((SELECT domain FROM sys_oss_config WHERE config_key = 'qcloud' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.domain')), ''),
                'endpoint', REPLACE(COALESCE((SELECT endpoint FROM sys_oss_config WHERE config_key = 'qcloud' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.endpoint')), ''), 'https://', ''),
                'secret_id', COALESCE((SELECT access_key FROM sys_oss_config WHERE config_key = 'qcloud' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.secret_id')), ''),
                'secret_key', COALESCE((SELECT secret_key FROM sys_oss_config WHERE config_key = 'qcloud' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.secret_key')), ''),
                'bucket', COALESCE((SELECT bucket_name FROM sys_oss_config WHERE config_key = 'qcloud' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.bucket')), ''),
                'region', COALESCE((SELECT region FROM sys_oss_config WHERE config_key = 'qcloud' LIMIT 1), JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.region')), 'ap-guangzhou')
            )
        )
    ),
    update_time = NOW()
WHERE service_type = 'oss' AND provider_code = 'tencent';

-- 5. 删除已经迁移完成的旧 sys_config 脏数据，保留 sms_open 等业务开关
DELETE FROM sys_config
WHERE config_key IN (
    'oss.provider',
    'oss.tencent.secretId',
    'oss.tencent.secretKey',
    'oss.tencent.bucket',
    'oss.tencent.region',
    'oss.tencent.domain',
    'oss.aliyun.accessKeyId',
    'oss.aliyun.accessKeySecret',
    'oss.aliyun.bucket',
    'oss.aliyun.endpoint',
    'chooseSms',
    'sms_access_key',
    'sms_access_secret',
    'sms_region',
    'sms_sign',
    'sms_templateconfig_id',
    'tencentSecretconfig_id',
    'tencentSecretKey',
    'tencentSmsRegion',
    'tencentSmsSdkAppconfig_id',
    'tencentSmsSignName',
    'tencentSmsTemplateconfig_id',
    'WxMpAppconfig_id',
    'WxMpAppId',
    'WxMpSecret',
    'WxAppId',
    'wxAppSecret',
    'wxAdpconfig_id',
    'app_appconfig_id',
    'wxPayKey',
    'wxPaySecret',
    'appNotifyurl',
    'redirectUrl',
    'CLOUD_STORAGE_CONFIG_KEY'
);

UPDATE sys_oss
SET service = 'tencent'
WHERE service = 'qcloud';

DROP TABLE IF EXISTS sys_oss_config;

-- 6. 删除工作流遗留字典脏数据，保留 sys_job_type
DELETE FROM sys_dict_data
WHERE dict_type IN ('wf_business_status', 'wf_form_type', 'wf_task_status');

DELETE FROM sys_dict_type
WHERE dict_type IN ('wf_business_status', 'wf_form_type', 'wf_task_status');

COMMIT;
