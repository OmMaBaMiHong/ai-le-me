-- =============================================
-- 认证菜单数据 - 添加实名认证和学历认证入口
-- 执行前请确认 user_menu 表结构和数据
-- =============================================

-- 查看现有菜单最大排序值
-- SELECT MAX(sort_order) FROM user_menu;

-- 添加实名认证菜单
INSERT INTO `user_menu` (`title`, `icon`, `url`, `sort_order`, `status`, `create_time`, `update_time`)
VALUES ('实名认证', 'https://wo.ai-ni.store/webstatic/auth/realname.png', '/subpages/auth/realname', 100, 1, NOW(), NOW());

-- 添加学历认证菜单
INSERT INTO `user_menu` (`title`, `icon`, `url`, `sort_order`, `status`, `create_time`, `update_time`)
VALUES ('学历认证', 'https://wo.ai-ni.store/webstatic/auth/education.png', '/subpages/auth/education', 101, 1, NOW(), NOW());

-- 注意：
-- 1. icon 图片需要上传到服务器，或使用本地路径如 /static/images/auth/realname.png
-- 2. sort_order 需要根据现有菜单调整，确保显示顺序合理
-- 3. status=1 表示启用，0 表示禁用

-- 如果需要使用本地图标，请先创建图标文件：
-- qiuou-uniapp/src/static/images/auth/realname.png
-- qiuou-uniapp/src/static/images/auth/education.png
-- 然后将 url 中的图标路径改为：/static/images/auth/realname.png
