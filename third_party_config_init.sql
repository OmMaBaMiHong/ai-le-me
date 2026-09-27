-- =====================================================
-- 第三方服务配置初始化SQL
-- 基于现有sys_config表扩展
-- =====================================================

-- 1. 扩展sys_config表字段(可选,如果表结构支持就添加)
-- ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS `config_group` varchar(50) DEFAULT 'system' COMMENT '配置分组' AFTER config_type;
-- ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS `is_sensitive` tinyint(1) DEFAULT 0 COMMENT '是否敏感(1=脱敏显示)' AFTER config_group;
-- ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS `value_type` varchar(20) DEFAULT 'text' COMMENT '值类型:text/password/number/boolean/select/json' AFTER is_sensitive;
-- ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS `select_options` varchar(500) DEFAULT NULL COMMENT 'select类型的可选项(JSON数组)' AFTER value_type;

-- =====================================================
-- 一、AI模型服务配置
-- =====================================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI模型-当前提供商', 'ai.persona.provider', 'mock', 'Y', 'AI画像提供商:mock(测试)/tongyi(通义千问)/zhipu(智谱GLM)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI模型-通义千问Key', 'ai.tongyi.api-key', '', 'Y', '阿里云DashScope API Key(敏感信息,获取地址:https://dashscope.console.aliyun.com/apiKey)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI模型-通义千问Model', 'ai.tongyi.model', 'qwen-turbo', 'Y', '通义千问模型版本:qwen-turbo(快速)/qwen-plus(平衡)/qwen-max(高质量)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = 'qwen-turbo', update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI模型-智谱GLMKey', 'ai.zhipu.api-key', '', 'Y', '智谱开放平台 API Key(敏感信息,获取地址:https://open.bigmodel.cn/usercenter/apikeys)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI模型-智谱GLMModel', 'ai.zhipu.model', 'glm-4', 'Y', '智谱GLM模型版本:glm-4(推荐)/glm-4-flash(快速)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = 'glm-4', update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI画像-生成费用', 'persona.generate.cost.integral', '100', 'Y', '生成AI画像消耗的积分数(非VIP用户)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = '100', update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI画像-VIP月免费次数', 'persona.generate.cost.vip-free-monthly', '3', 'Y', 'VIP用户每月免费生成AI画像的次数', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = '3', update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('AI画像-查看费用', 'persona.view.cost.integral', '50', 'Y', '查看他人AI画像消耗的积分数(解锁后永久可看)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = '50', update_time = NOW();

-- =====================================================
-- 二、云存储服务配置(OSS)
-- =====================================================
-- OSS 已统一迁移到 sys_third_party_provider(service_type='oss')。
-- 不再初始化任何 sys_config 下的 oss.* 旧配置，避免新环境继续写入旧链路。

-- =====================================================
-- 三、短信服务配置
-- =====================================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('短信服务-启用状态', 'sms.enabled', 'true', 'Y', '是否启用短信服务:true/false', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('短信服务-当前提供商', 'sms.provider', 'tencent', 'Y', '短信提供商:tencent(腾讯云)/aliyun(阿里云)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 腾讯云短信
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云短信-AppId', 'sms.tencent.appId', '', 'Y', '腾讯云短信应用ID', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云短信-SecretId', 'sms.tencent.secretId', '', 'Y', '腾讯云SecretId(敏感信息)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云短信-SecretKey', 'sms.tencent.secretKey', '', 'Y', '腾讯云SecretKey(敏感信息)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云短信-签名', 'sms.tencent.signName', '', 'Y', '腾讯云短信签名内容(需在腾讯云后台审核通过)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云短信-登录模板ID', 'sms.tencent.template.login', '', 'Y', '登录验证码短信模板ID', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯云短信-通知模板ID', 'sms.tencent.template.notify', '', 'Y', '通知类短信模板ID', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- =====================================================
-- 四、支付服务配置
-- =====================================================
-- 微信支付
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('微信支付-启用状态', 'pay.wechat.enabled', 'true', 'Y', '是否启用微信支付:true/false', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('微信支付-商户号', 'pay.wechat.mchId', '', 'Y', '微信支付商户号(敏感信息)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('微信支付-APIKey', 'pay.wechat.apiKey', '', 'Y', '微信支付API密钥(敏感信息,在商户平台设置)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('微信支付-AppId', 'pay.wechat.appId', '', 'Y', '微信公众号/小程序AppId', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('微信支付-回调域名', 'pay.wechat.notifyUrl', '', 'Y', '微信支付异步通知回调地址(公网可访问)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 支付宝
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('支付宝-启用状态', 'pay.alipay.enabled', 'false', 'Y', '是否启用支付宝支付:true/false', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('支付宝-AppId', 'pay.alipay.appId', '', 'Y', '支付宝应用ID(在开放平台创建应用获取)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('支付宝-应用私钥', 'pay.alipay.privateKey', '', 'Y', '支付宝应用私钥(敏感信息,RSA2格式)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('支付宝-支付宝公钥', 'pay.alipay.alipayPublicKey', '', 'Y', '支付宝公钥(在开放平台获取)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- =====================================================
-- 五、推送服务配置(新增)
-- =====================================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('推送服务-启用状态', 'push.enabled', 'false', 'Y', '是否启用推送服务:true/false', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('推送服务-当前提供商', 'push.provider', 'jpush', 'Y', '推送提供商:jpush(极光)/unipush(UniPush)/umeng(友盟)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 极光推送
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('极光推送-AppKey', 'push.jpush.appKey', '', 'Y', '极光推送AppKey(敏感信息)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('极光推送-MasterSecret', 'push.jpush.masterSecret', '', 'Y', '极光推送MasterSecret(敏感信息)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- =====================================================
-- 六、地图服务配置(新增)
-- =====================================================
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('地图服务-当前提供商', 'map.provider', 'tencent', 'Y', '地图提供商:tencent(腾讯)/amap(高德)/baidu(百度)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('腾讯地图-Key', 'map.tencent.key', '', 'Y', '腾讯地图开发者Key(在腾讯位置服务申请)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('高德地图-Key', 'map.amap.key', '', 'Y', '高德地图开发者Key(Web服务Key)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('百度地图-AK', 'map.baidu.ak', '', 'Y', '百度地图开发者AK(在百度地图开放平台申请)', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- =====================================================
-- 七、菜单权限配置(添加到系统菜单)
-- =====================================================
-- 查询父菜单ID(系统管理)
SET @parent_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1);

-- 插入"第三方服务"菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('第三方服务', @parent_menu_id, 8, 'thirdparty', 'system/thirdparty/index', 1, 0, 'C', '0', '0', 'system:thirdparty:list', 'cloud', 'admin', NOW(), 'admin', NOW(), '第三方服务配置管理')
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 获取刚插入的菜单ID
SET @thirdparty_menu_id = LAST_INSERT_ID();

-- 插入子菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('AI模型配置', @thirdparty_menu_id, 1, 'ai', 'system/thirdparty/ai', 'C', '0', '0', 'system:thirdparty:ai', 'admin', NOW()),
('云存储配置', @thirdparty_menu_id, 2, 'oss', 'system/thirdparty/oss', 'C', '0', '0', 'system:thirdparty:oss', 'admin', NOW()),
('短信服务配置', @thirdparty_menu_id, 3, 'sms', 'system/thirdparty/sms', 'C', '0', '0', 'system:thirdparty:sms', 'admin', NOW()),
('支付服务配置', @thirdparty_menu_id, 4, 'payment', 'system/thirdparty/payment', 'C', '0', '0', 'system:thirdparty:payment', 'admin', NOW()),
('推送服务配置', @thirdparty_menu_id, 5, 'push', 'system/thirdparty/push', 'C', '0', '0', 'system:thirdparty:push', 'admin', NOW()),
('地图服务配置', @thirdparty_menu_id, 6, 'map', 'system/thirdparty/map', 'C', '0', '0', 'system:thirdparty:map', 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- 插入按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
('第三方服务查询', @thirdparty_menu_id, 1, 'F', '0', '0', 'system:thirdparty:query', 'admin', NOW()),
('第三方服务修改', @thirdparty_menu_id, 2, 'F', '0', '0', 'system:thirdparty:edit', 'admin', NOW()),
('第三方服务测试', @thirdparty_menu_id, 3, 'F', '0', '0', 'system:thirdparty:test', 'admin', NOW()),
('查看敏感信息', @thirdparty_menu_id, 4, 'F', '0', '0', 'system:thirdparty:sensitive', 'admin', NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

-- =====================================================
-- 执行完成提示
-- =====================================================
SELECT '第三方服务配置初始化完成!' AS message,
       '请在后台管理系统"系统管理 -> 第三方服务"中配置相关参数' AS next_step;
