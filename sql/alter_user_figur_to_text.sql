ALTER TABLE `user`
    MODIFY COLUMN `figur` TEXT
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci
    NULL
    COMMENT '用户形象图，支持多张图片 URL';
