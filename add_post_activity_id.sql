ALTER TABLE `post`
    ADD COLUMN `activity_id` bigint NULL COMMENT '关联活动ID' AFTER `discuss_id`,
    ADD INDEX `idx_post_activity_id` (`activity_id`);
