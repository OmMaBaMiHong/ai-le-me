ALTER TABLE `post`
    MODIFY COLUMN `content` TEXT
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci
    NULL
    COMMENT '帖子正文，支持长文';
