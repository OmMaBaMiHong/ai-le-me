 
  1101
  -- 创建帖子话题关联表
CREATE TABLE IF NOT EXISTS `post_tags` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint(20) NOT NULL COMMENT '帖子ID',
  `tag_id` bigint(20) NOT NULL COMMENT '话题ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子话题关联表';

-- 确保tags表有usageCount字段
ALTER TABLE `tags` ADD COLUMN IF NOT EXISTS `usage_count` int(11) DEFAULT '0' COMMENT '使用次数';
ALTER TABLE `tags` ADD COLUMN IF NOT EXISTS `follower_count` int(11) DEFAULT '0' COMMENT '关注人数';
ALTER TABLE `tags` ADD COLUMN IF NOT EXISTS `status` tinyint(1) DEFAULT '1' COMMENT '状态：0-禁用 1-启用';
ALTER TABLE `tags` ADD COLUMN IF NOT EXISTS `tag_desc` varchar(500) DEFAULT NULL COMMENT '话题描述';

-- 初始化一些热门话题数据
INSERT INTO `tags` (`id`, `tag_name`, `tag_category`, `tag_desc`, `usage_count`, `follower_count`, `status`, `create_time`) VALUES
(1, '单身日记', '情感', '记录单身生活的点点滴滴', 0, 0, 1, NOW()),
(2, '恋爱脑', '情感', '分享恋爱中的甜蜜瞬间', 0, 0, 1, NOW()),
(3, '美食探店', '美食', '发现城市里的美味', 0, 0, 1, NOW()),
(4, '健身打卡', '运动', '记录健身的每一天', 0, 0, 1, NOW()),
(5, '旅行vlog', '生活', '用镜头记录旅途', 0, 0, 1, NOW()),
(6, '读书分享', '兴趣', '好书推荐与读后感', 0, 0, 1, NOW()),
(7, '电影推荐', '娱乐', '分享值得一看的影片', 0, 0, 1, NOW()),
(8, '穿搭日常', '生活', '今天的穿搭分享', 0, 0, 1, NOW()),
(9, '宠物日常', '生活', '晒晒我的毛孩子', 0, 0, 1, NOW()),
(10, '职场故事', '生活', '工作中的酸甜苦辣', 0, 0, 1, NOW())
ON DUPLICATE KEY UPDATE tag_name=VALUES(tag_name);
