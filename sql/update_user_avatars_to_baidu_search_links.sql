-- 将 user 表头像替换为百度图片搜索页中抓取到的 img.baidu.com 直链
-- 当前 gender 分布保持不变：gender=1 用 male 列表，gender=2 用 female 列表
-- 适用库：bang_yi

UPDATE `user`
SET `avatar` = CASE
    WHEN `gender` = 1 THEN CASE MOD(`uid`, 20)
        WHEN 1 THEN 'https://img0.baidu.com/it/u=3727516819,1861485078&fm=253&fmt=auto&app=138&f=JPEG?w=870&h=800'
        WHEN 2 THEN 'https://img2.baidu.com/it/u=4240068872,3712614126&fm=253&app=138&f=JPEG?w=646&h=599'
        WHEN 3 THEN 'https://img0.baidu.com/it/u=382352698,1399454437&fm=253&app=138&f=JPEG?w=800&h=882'
        WHEN 4 THEN 'https://img2.baidu.com/it/u=278878039,253574776&fm=253&app=138&f=JPEG?w=869&h=800'
        WHEN 5 THEN 'https://img0.baidu.com/it/u=2154652553,2383431160&fm=253&fmt=auto&app=138&f=JPEG?w=507&h=500'
        WHEN 6 THEN 'https://img0.baidu.com/it/u=3240677672,415517454&fm=253&app=138&f=JPEG?w=500&h=562'
        WHEN 7 THEN 'https://img0.baidu.com/it/u=3818057155,704305936&fm=253&app=138&f=JPEG?w=512&h=500'
        WHEN 8 THEN 'https://img2.baidu.com/it/u=1423922963,558452063&fm=253&app=138&f=JPEG?w=521&h=500'
        WHEN 9 THEN 'https://img1.baidu.com/it/u=829354742,2433209838&fm=253&fmt=auto&app=138&f=JPEG?w=504&h=500'
        WHEN 10 THEN 'https://img0.baidu.com/it/u=925931293,1503343837&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 11 THEN 'https://img2.baidu.com/it/u=4243367371,1764649471&fm=253&fmt=auto&app=138&f=JPEG?w=380&h=380'
        WHEN 12 THEN 'https://img0.baidu.com/it/u=831403021,3902266642&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=501'
        WHEN 13 THEN 'https://img1.baidu.com/it/u=1587667128,3380142508&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 14 THEN 'https://img1.baidu.com/it/u=61345885,703721898&fm=253&fmt=auto&app=138&f=JPEG?w=380&h=380'
        WHEN 15 THEN 'https://img1.baidu.com/it/u=1772908248,3182067950&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 16 THEN 'https://img1.baidu.com/it/u=434861999,1785800061&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 17 THEN 'https://img0.baidu.com/it/u=1651140820,1781376921&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 18 THEN 'https://img0.baidu.com/it/u=3804884912,2675857087&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 19 THEN 'https://img2.baidu.com/it/u=3252513699,3529574848&fm=253&app=138&f=JPEG?w=500&h=500'
        ELSE 'https://img2.baidu.com/it/u=1551839510,594912236&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
    END
    WHEN `gender` = 2 THEN CASE MOD(`uid`, 20)
        WHEN 1 THEN 'https://img1.baidu.com/it/u=2616858463,3646651699&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=584'
        WHEN 2 THEN 'https://img1.baidu.com/it/u=145904989,1771414628&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 3 THEN 'https://img2.baidu.com/it/u=2530111115,2139169352&fm=253&app=138&f=JPEG?w=509&h=500'
        WHEN 4 THEN 'https://img0.baidu.com/it/u=2388615347,3739939155&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 5 THEN 'https://img1.baidu.com/it/u=3074644945,854122769&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=524'
        WHEN 6 THEN 'https://img0.baidu.com/it/u=3516060725,1694884512&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 7 THEN 'https://img1.baidu.com/it/u=905240344,1104663529&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 8 THEN 'https://img2.baidu.com/it/u=2019204321,2317660844&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 9 THEN 'https://img0.baidu.com/it/u=1842705353,3171208175&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 10 THEN 'https://img1.baidu.com/it/u=1683389318,236818286&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 11 THEN 'https://img2.baidu.com/it/u=94253712,3409731556&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 12 THEN 'https://img0.baidu.com/it/u=2645433111,2218976545&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 13 THEN 'https://img0.baidu.com/it/u=665233224,408706864&fm=253&app=138&f=JPEG?w=500&h=500'
        WHEN 14 THEN 'https://img2.baidu.com/it/u=690224922,1664915598&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 15 THEN 'https://img0.baidu.com/it/u=1825774851,3910170653&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 16 THEN 'https://img0.baidu.com/it/u=2370318113,2888648240&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=800'
        WHEN 17 THEN 'https://img2.baidu.com/it/u=1505569278,2408996465&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        WHEN 18 THEN 'https://img1.baidu.com/it/u=2620993718,3722308193&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=553'
        WHEN 19 THEN 'https://img0.baidu.com/it/u=4076448576,3851498415&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
        ELSE 'https://img1.baidu.com/it/u=3097831896,2920478992&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'
    END
    ELSE `avatar`
END;

SELECT `gender`, COUNT(*) AS `count`
FROM `user`
GROUP BY `gender`
ORDER BY `gender`;
