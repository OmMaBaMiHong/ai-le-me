-- 实名认证与学历认证 sys_config 配置初始化脚本
-- 在若依后台「系统管理 → 参数设置」中，点击「实名认证配置」或「学历认证配置」按钮即可筛选管理

-- ================================================
-- 实名认证配置（阿里云 + 腾讯云双备份）
-- ================================================

-- 1. 实名认证服务商选择（aliyun 或 tencent）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('实名认证服务商', 'realname.provider', 'aliyun', 'N', '可选值: aliyun(阿里云) / tencent(腾讯云) / baidu(百度云)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = 'aliyun', update_time = NOW();

-- 2. 阿里云实名认证配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('阿里云AccessKeyId', 'realname.aliyun.accessKeyId', '', 'N', '阿里云实人认证 AccessKeyId', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('阿里云AccessKeySecret', 'realname.aliyun.accessKeySecret', '', 'N', '阿里云实人认证 AccessKeySecret（敏感信息）', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('阿里云实名认证Endpoint', 'realname.aliyun.endpoint', 'cloudauth.aliyuncs.com', 'N', '阿里云实人认证服务endpoint（默认无需修改）', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = 'cloudauth.aliyuncs.com', update_time = NOW();

-- 3. 腾讯云实名认证配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云SecretId', 'realname.tencent.secretId', '', 'N', '腾讯云人脸核身 SecretId', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云SecretKey', 'realname.tencent.secretKey', '', 'N', '腾讯云人脸核身 SecretKey（敏感信息）', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云实名认证Region', 'realname.tencent.region', 'ap-guangzhou', 'N', '腾讯云人脸核身地域（默认: ap-guangzhou）', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = 'ap-guangzhou', update_time = NOW();

-- ================================================
-- 学历认证配置
-- ================================================

-- 4. 学历认证服务商选择
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('学历认证服务商', 'education.provider', 'chsi', 'N', '可选值: chsi(学信网) / aliyun(阿里云) / baidu(百度云) / custom(自定义)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = 'chsi', update_time = NOW();

-- 5. 学信网配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('学信网查询URL', 'education.chsi.url', '', 'N', '学信网学历查询中间服务URL（可选，留空则用默认）', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 6. 阿里云学历认证配置（预留）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('阿里云学历认证URL', 'education.aliyun.url', '', 'N', '阿里云市场学历查询服务URL', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 7. 百度云学历认证配置（预留）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('百度云学历认证URL', 'education.baidu.url', '', 'N', '百度云学历查询服务URL', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 8. 自定义学历认证配置（预留）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('自定义学历认证URL', 'education.api.url', '', 'N', '自定义学历查询服务URL', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- ================================================
-- 配置说明
-- ================================================
-- 1. 实名认证默认使用「阿里云」，你需要填写 realname.aliyun.accessKeyId 和 realname.aliyun.accessKeySecret
-- 2. 如需切换到「腾讯云」，修改 realname.provider = tencent，并填写对应的 secretId 和 secretKey
-- 3. 学历认证默认使用「学信网」模式（前端传验证码），暂无需额外配置
-- 4. 配置完成后在后台「参数设置」点击「刷新缓存」使配置生效
-- 5. 前端调用接口示例：
--    POST /app/user/realName
--    参数: name=张三&idCard=440112199001011234
