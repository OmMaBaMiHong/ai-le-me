-- 将 user 表中的外籍示例头像替换为本地静态目录下的百度系亚洲男女头像
-- 适用库：bang_yi

UPDATE `user`
SET `avatar` = CASE
    WHEN `gender` = 1 THEN CASE MOD(`uid`, 10)
        WHEN 1 THEN '/static/seed-avatar/baidu/male-01.jpg'
        WHEN 2 THEN '/static/seed-avatar/baidu/male-02.jpg'
        WHEN 3 THEN '/static/seed-avatar/baidu/male-03.jpg'
        WHEN 4 THEN '/static/seed-avatar/baidu/male-04.jpg'
        WHEN 5 THEN '/static/seed-avatar/baidu/male-05.jpg'
        WHEN 6 THEN '/static/seed-avatar/baidu/male-06.jpg'
        WHEN 7 THEN '/static/seed-avatar/baidu/male-07.jpg'
        WHEN 8 THEN '/static/seed-avatar/baidu/male-08.jpg'
        WHEN 9 THEN '/static/seed-avatar/baidu/male-09.jpg'
        ELSE '/static/seed-avatar/baidu/male-10.jpg'
    END
    WHEN `gender` = 2 THEN CASE MOD(`uid`, 8)
        WHEN 1 THEN '/static/seed-avatar/baidu/female-01.jpg'
        WHEN 2 THEN '/static/seed-avatar/baidu/female-02.jpg'
        WHEN 3 THEN '/static/seed-avatar/baidu/female-03.jpg'
        WHEN 4 THEN '/static/seed-avatar/baidu/female-04.jpg'
        WHEN 5 THEN '/static/seed-avatar/baidu/female-05.jpg'
        WHEN 6 THEN '/static/seed-avatar/baidu/female-06.jpg'
        WHEN 7 THEN '/static/seed-avatar/baidu/female-07.jpg'
        ELSE '/static/seed-avatar/baidu/female-08.jpg'
    END
    ELSE `avatar`
END
WHERE `avatar` LIKE '%randomuser.me%'
   OR `avatar` LIKE '%unsplash.com%';

SELECT `gender`, COUNT(*) AS `count`
FROM `user`
WHERE `avatar` LIKE '/static/seed-avatar/baidu/%'
GROUP BY `gender`
ORDER BY `gender`;
