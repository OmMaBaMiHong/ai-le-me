USE bang_yi;

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'payment', 'alipay', '支付宝支付', 1, 1, 2,
       'https://open.alipay.com',
       'https://opendocs.alipay.com/open/203/105285',
       '支付宝运行时配置',
       '{}',
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'payment' AND provider_code = 'alipay'
);

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'app_id', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'pay.alipay.appId' LIMIT 1), ''),
            'sign_type', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'pay.alipay.sign_type' LIMIT 1), 'RSA2'),
            'private_key', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'pay.alipay.privateKey' LIMIT 1), ''),
            'alipay_public_key', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'pay.alipay.alipayPublicKey' LIMIT 1), ''),
            'notify_url', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'pay.alipay.notify_url' LIMIT 1), ''),
            'return_url', COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'pay.alipay.return_url' LIMIT 1), ''),
            'scenarios', JSON_ARRAY('app', 'h5')
        ),
        JSON_OBJECT(
            '_meta', JSON_OBJECT(
                'app_id', JSON_OBJECT('configLabel', '应用ID', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', '支付宝开放平台 AppId'),
                'sign_type', JSON_OBJECT('configLabel', '签名类型', 'defaultValue', 'RSA2', 'valueType', 'select', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 2, 'selectOptions', '["RSA2","RSA"]', 'helpText', '推荐使用 RSA2'),
                'private_key', JSON_OBJECT('configLabel', '应用私钥', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 3, 'helpText', '支付宝应用私钥'),
                'alipay_public_key', JSON_OBJECT('configLabel', '支付宝公钥', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 4, 'helpText', '支付宝平台公钥'),
                'notify_url', JSON_OBJECT('configLabel', '异步回调地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 5, 'helpText', '支付结果异步通知地址'),
                'return_url', JSON_OBJECT('configLabel', '同步返回地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 6, 'helpText', '支付完成后前端跳转地址'),
                'scenarios', JSON_OBJECT('configLabel', '支付场景', 'defaultValue', '["app","h5"]', 'valueType', 'json', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 7, 'helpText', '支持的支付场景列表')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'payment' AND provider_code = 'alipay';
