INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'wechat_mini', 'wechat', '微信小程序', 1, 1, 1,
       'https://developers.weixin.qq.com/miniprogram/dev/framework/',
       'https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/',
       '微信小程序运行时配置',
       '{}',
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'wechat_mini' AND provider_code = 'wechat'
);

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'wechat_mp', 'wechat', '微信公众号', 1, 1, 1,
       'https://mp.weixin.qq.com',
       'https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Overview.html',
       '微信公众号运行时配置',
       '{}',
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'wechat_mp' AND provider_code = 'wechat'
);

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'wechat_app', 'wechat', '微信开放应用', 1, 1, 1,
       'https://open.weixin.qq.com',
       'https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Access_Guide/Development_Guide.html',
       '微信 App 支付配置',
       '{}',
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'wechat_app' AND provider_code = 'wechat'
);

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'WxAppId' LIMIT 1), ''),
            'app_secret', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxAppSecret' LIMIT 1), ''),
            'ad_pid', COALESCE(
                (SELECT config_value FROM sys_config WHERE config_key = 'wxAdpid' LIMIT 1),
                (SELECT config_value FROM sys_config WHERE config_key = 'wxAdpconfig_id' LIMIT 1),
                ''
            )
        ),
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'app_id', JSON_OBJECT('configLabel', '小程序 AppId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', '微信小程序 AppId'),
                'app_secret', JSON_OBJECT('configLabel', '小程序 AppSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 2, 'helpText', '微信小程序密钥'),
                'ad_pid', JSON_OBJECT('configLabel', '微信广告位 ID', 'defaultValue', '', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 3, 'helpText', '小程序流量主广告位 ID')
            )
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
                ''
            ),
            'app_secret', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'WxMpSecret' LIMIT 1), ''),
            'token', ''
        ),
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'app_id', JSON_OBJECT('configLabel', '公众号 AppId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', '微信公众号 AppId'),
                'app_secret', JSON_OBJECT('configLabel', '公众号 AppSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 2, 'helpText', '微信公众号密钥'),
                'token', JSON_OBJECT('configLabel', '公众号 Token', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 3, 'helpText', '微信公众号服务端校验 Token')
            )
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
            'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'app_appid' LIMIT 1), ''),
            'app_secret', ''
        ),
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'app_id', JSON_OBJECT('configLabel', '开放平台 AppId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 1, 'helpText', '微信开放平台移动应用 AppId'),
                'app_secret', JSON_OBJECT('configLabel', '开放平台 AppSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 0, 'isSensitive', 1, 'displayOrder', 2, 'helpText', '微信开放平台应用密钥')
            )
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
            'mch_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxPayKey' LIMIT 1), ''),
            'api_key', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'wxPaySecret' LIMIT 1), ''),
            'notify_url', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'appNotifyurl' LIMIT 1), '')
        ),
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'mch_id', JSON_OBJECT('configLabel', '商户号', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', '微信支付商户号'),
                'api_key', JSON_OBJECT('configLabel', 'API密钥(v2)', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 3, 'helpText', '微信支付 API 密钥'),
                'notify_url', JSON_OBJECT('configLabel', '支付回调地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 5, 'helpText', '微信支付异步回调地址')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'payment' AND provider_code = 'wechat';
