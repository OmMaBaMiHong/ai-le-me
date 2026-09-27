-- VIP会员权益设置表
CREATE TABLE IF NOT EXISTS `vip_benefit` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(100) DEFAULT NULL COMMENT '权益标题',
  `describes` varchar(255) DEFAULT NULL COMMENT '权益描述',
  `icon` varchar(500) DEFAULT NULL COMMENT '图标URL',
  `status` int DEFAULT '0' COMMENT '状态 0-有效 1-无效',
  `sort` int DEFAULT '0' COMMENT '排序（数值越大越靠前）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='VIP会员权益设置';

-- 插入示例数据
INSERT INTO `vip_benefit` (`title`, `describes`, `icon`, `status`, `sort`, `create_time`, `update_time`) VALUES
('专属标识', '个人主页显示VIP专属标识', '', 0, 100, NOW(), NOW()),
('无限发帖', '每日发帖数量不受限制', '', 0, 90, NOW(), NOW()),
('优先推荐', '动态优先展示在首页推荐位', '', 0, 80, NOW(), NOW()),
('查看访客', '查看谁访问了我的主页', '', 0, 70, NOW(), NOW()),
('高级筛选', '使用高级筛选功能精准匹配', '', 0, 60, NOW(), NOW());
