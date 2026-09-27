USE bang_yi;

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'social_gitea', 'gitea', 'Gitea 登录', 1, 1, 1,
       'https://about.gitea.com',
       'https://docs.gitea.com',
       'Gitea 社交登录配置',
       JSON_OBJECT(),
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'social_gitea' AND provider_code = 'gitea'
);

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'social_maxkey', 'maxkey', 'MaxKey 登录', 1, 1, 1,
       'https://www.maxkey.top',
       'https://www.maxkey.top/zh/docs/',
       'MaxKey 社交登录配置',
       JSON_OBJECT(),
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'social_maxkey' AND provider_code = 'maxkey'
);

INSERT INTO sys_third_party_provider
    (service_type, provider_code, provider_name, is_current, is_enabled, display_order, official_website, doc_url, remark, config_json, create_time, update_time)
SELECT 'social_topiam', 'topiam', 'TopIAM 登录', 1, 1, 1,
       'https://topiam.cn',
       'https://topiam.cn/docs',
       'TopIAM 社交登录配置',
       JSON_OBJECT(),
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_third_party_provider WHERE service_type = 'social_topiam' AND provider_code = 'topiam'
);

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'client_id', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.client_id')), ''),
            'client_secret', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.client_secret')), ''),
            'server_url', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.server_url')), ''),
            '_meta', JSON_OBJECT(
                'client_id', JSON_OBJECT('configLabel', 'ClientId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', 'OAuth ClientId'),
                'client_secret', JSON_OBJECT('configLabel', 'ClientSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 2, 'helpText', 'OAuth ClientSecret'),
                'server_url', JSON_OBJECT('configLabel', '服务端地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 3, 'helpText', 'OAuth 服务端基地址')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'social_gitea' AND provider_code = 'gitea';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'client_id', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.client_id')), ''),
            'client_secret', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.client_secret')), ''),
            'server_url', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.server_url')), ''),
            '_meta', JSON_OBJECT(
                'client_id', JSON_OBJECT('configLabel', 'ClientId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', 'OAuth ClientId'),
                'client_secret', JSON_OBJECT('configLabel', 'ClientSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 2, 'helpText', 'OAuth ClientSecret'),
                'server_url', JSON_OBJECT('configLabel', '服务端地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 3, 'helpText', 'MaxKey 服务端基地址')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'social_maxkey' AND provider_code = 'maxkey';

UPDATE sys_third_party_provider
SET config_json = JSON_PRETTY(
    JSON_MERGE_PATCH(
        IF(JSON_VALID(config_json), JSON_EXTRACT(config_json, '$'), JSON_OBJECT()),
        JSON_OBJECT(
            'client_id', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.client_id')), ''),
            'client_secret', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.client_secret')), ''),
            'server_url', COALESCE(JSON_UNQUOTE(JSON_EXTRACT(config_json, '$.server_url')), ''),
            'scopes', COALESCE(JSON_EXTRACT(config_json, '$.scopes'), JSON_ARRAY('openid', 'email', 'phone', 'profile')),
            '_meta', JSON_OBJECT(
                'client_id', JSON_OBJECT('configLabel', 'ClientId', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 1, 'helpText', 'OAuth ClientId'),
                'client_secret', JSON_OBJECT('configLabel', 'ClientSecret', 'defaultValue', '', 'valueType', 'password', 'isRequired', 1, 'isSensitive', 1, 'displayOrder', 2, 'helpText', 'OAuth ClientSecret'),
                'server_url', JSON_OBJECT('configLabel', '服务端地址', 'defaultValue', '', 'valueType', 'text', 'isRequired', 1, 'isSensitive', 0, 'displayOrder', 3, 'helpText', 'TopIAM 服务端基地址'),
                'scopes', JSON_OBJECT('configLabel', 'Scopes', 'defaultValue', '', 'valueType', 'textarea', 'isRequired', 0, 'isSensitive', 0, 'displayOrder', 4, 'helpText', 'JSON 数组或逗号分隔')
            )
        )
    )
),
update_time = NOW()
WHERE service_type = 'social_topiam' AND provider_code = 'topiam';
