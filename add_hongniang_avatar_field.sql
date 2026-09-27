-- 添加红娘头像字段
ALTER TABLE `hongniang_info` 
ADD COLUMN `avatar` varchar(500) DEFAULT NULL COMMENT '红娘头像' AFTER `hongniang_name`;

-- 如果需要，可以从关联的用户表同步头像数据
-- UPDATE hongniang_info h 
-- INNER JOIN app_user u ON h.user_id = u.uid 
-- SET h.avatar = u.avatar 
-- WHERE h.user_id IS NOT NULL AND u.avatar IS NOT NULL;
