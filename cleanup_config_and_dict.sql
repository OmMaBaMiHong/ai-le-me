-- 配置/字典收敛检查与清理脚本
-- 执行前建议先备份 bang_yi.sys_config / sys_dict_type / sys_dict_data

-- 1) 当前仍残留在 sys_config、但应由 third 配置统一管理的 key
-- 正式执行请使用 config_third_dict_cleanup_migration.sql
SELECT config_key, config_name, config_type, config_value
FROM sys_config
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
    'sms_sign',
    'sms_templateconfig_id',
    'sms_access_secret',
    'sms_access_key',
    'sms_region',
    'chooseSms',
    'tencentSecretconfig_id',
    'tencentSecretKey',
    'tencentSmsRegion',
    'tencentSmsSdkAppconfig_id',
    'tencentSmsSignName',
    'tencentSmsTemplateconfig_id',
    'WxMpAppconfig_id',
    'WxMpSecret',
    'WxAppId',
    'wxAppSecret',
    'wxPayKey',
    'wxPaySecret',
    'appNotifyurl',
    'redirectUrl',
    'CLOUD_STORAGE_CONFIG_KEY'
)
ORDER BY config_key;

-- 2) 工作流页面已删除后，wf_* 字典可一并清理
SELECT dict_type, dict_name, remark
FROM sys_dict_type
WHERE dict_type IN ('wf_business_status', 'wf_form_type', 'wf_task_status');

SELECT dict_type, dict_label, dict_value
FROM sys_dict_data
WHERE dict_type IN ('wf_business_status', 'wf_form_type', 'wf_task_status')
ORDER BY dict_type, dict_sort;

-- 3) 如果确认当前环境已不再使用工作流，可执行下列删除
-- 4) 如果 OSS 已迁移到 sys_third_party_provider，可执行下列清理
-- DELETE FROM sys_config
-- WHERE config_key IN (
--     'oss.provider',
--     'oss.tencent.secretId',
--     'oss.tencent.secretKey',
--     'oss.tencent.bucket',
--     'oss.tencent.region',
--     'oss.tencent.domain',
--     'oss.aliyun.accessKeyId',
--     'oss.aliyun.accessKeySecret',
--     'oss.aliyun.bucket',
--     'oss.aliyun.endpoint',
--     'CLOUD_STORAGE_CONFIG_KEY'
-- );
--
-- UPDATE sys_oss
-- SET service = 'tencent'
-- WHERE service = 'qcloud';
--
-- DROP TABLE IF EXISTS sys_oss_config;
--
-- DELETE FROM sys_dict_data
-- WHERE dict_type IN ('wf_business_status', 'wf_form_type', 'wf_task_status');
--
-- DELETE FROM sys_dict_type
-- WHERE dict_type IN ('wf_business_status', 'wf_form_type', 'wf_task_status');
