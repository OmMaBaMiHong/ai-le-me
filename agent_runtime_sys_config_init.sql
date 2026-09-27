INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES
('Agent Runtime 总开关', 'agent.runtime.enabled', '1', 'N', 'Python Agent Runtime 总开关：1开启 0关闭', 'admin', NOW(), 'admin', NOW()),
('Agent Runtime 地址', 'agent.runtime.baseUrl', 'http://127.0.0.1:8091', 'N', 'Python Agent Runtime 服务地址', 'admin', NOW(), 'admin', NOW()),
('Agent Runtime 画像开关', 'agent.runtime.persona.enabled', '1', 'N', '增强画像能力开关：1开启 0关闭', 'admin', NOW(), 'admin', NOW()),
('Agent Runtime 陪伴开关', 'agent.runtime.companion.enabled', '1', 'N', '智能陪伴建议开关：1开启 0关闭', 'admin', NOW(), 'admin', NOW()),
('AI礼物模板目录', 'agent_gift_catalog', '[{"code":"milk_tea","name":"爱你的第一杯奶茶","desc":"先把气氛变甜一点","scene":"轻松破冰","icon":"🧋","theme":"tea","templateCode":"tea_coupon","animationPreset":"tea_float","revealEffect":"bubble_pop","amount":1314,"recommended":true},{"code":"rose_bloom","name":"今晚这束花","desc":"把今天的好感认真递给你","scene":"表达认真","icon":"🌹","theme":"rose","templateCode":"rose_growth","animationPreset":"rose_bloom","revealEffect":"petal_burst","amount":5200},{"code":"starlight","name":"想你的星光","desc":"把今晚的好感点亮","scene":"升温试探","icon":"✨","theme":"star","templateCode":"starlight_orb","animationPreset":"orb_spin","revealEffect":"starburst","amount":13140},{"code":"moonlight","name":"晚安月光","desc":"把今晚的温柔和晚安一起送给你","scene":"轻柔陪伴","icon":"🌙","theme":"night","templateCode":"moonbox_reveal","animationPreset":"moon_drift","revealEffect":"moon_glow","amount":3340}]', 'N', 'AI礼物目录配置，支持 templateCode / animationPreset / revealEffect / soundEffectKey / soundEffectUrl 等模板字段，可按 JSON 继续扩展更多礼物模板', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE
config_value = VALUES(config_value),
remark = VALUES(remark),
update_by = VALUES(update_by),
update_time = VALUES(update_time);
