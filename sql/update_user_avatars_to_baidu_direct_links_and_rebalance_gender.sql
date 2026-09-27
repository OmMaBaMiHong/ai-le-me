-- 将 user 表头像统一替换为百度系直链，并把原 41 个本地男头像中的前 30 个改为女
-- 适用库：bang_yi

UPDATE `user`
SET `gender` = 2
WHERE `uid` IN (
    216, 217, 219, 221, 225, 226, 231, 232, 233, 236,
    241, 242, 244, 245, 246, 247, 249, 252, 255, 256,
    258, 260, 262, 264, 267, 273, 275, 277, 280, 283
);

UPDATE `user`
SET `avatar` = CASE
    WHEN `gender` = 1 THEN CASE MOD(`uid`, 10)
        WHEN 1 THEN 'https://bkimg.cdn.bcebos.com/pic/c8ea15ce36d3d539b60010cb2dd4fe50352ac65c1a22?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 2 THEN 'https://bkimg.cdn.bcebos.com/pic/63d9f2d3572c11dfa9ecbfe3b97375d0f703908f64ec?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 3 THEN 'https://bkimg.cdn.bcebos.com/pic/d788d43f8794a4c27d1eecf574a80cd5ad6edcc493e3?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 4 THEN 'https://bkimg.cdn.bcebos.com/pic/fcfaaf51f3deb48f8c5458fe5d442d292df5e0fed2c5?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 5 THEN 'https://bkimg.cdn.bcebos.com/pic/2934349b033b5bb5c9ea6d1af38bc239b6003bf31684?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 6 THEN 'https://bkimg.cdn.bcebos.com/pic/ae51f3deb48f8c5494ee818633713af5e0fe9925d158?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 7 THEN 'https://bkimg.cdn.bcebos.com/pic/78310a55b319ebc4ad5d97db8c26cffc1e17164e?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 8 THEN 'https://bkimg.cdn.bcebos.com/pic/f603918fa0ec08fa513df2fd1ab62a6d55fbb2fb6c6a?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 9 THEN 'https://bkimg.cdn.bcebos.com/pic/a08b87d6277f9e2f070894db5d6bfe24b899a8015596?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        ELSE 'https://bkimg.cdn.bcebos.com/pic/2fdda3cc7cd98d1001e9d800a167af0e7bec54e73af3?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
    END
    WHEN `gender` = 2 THEN CASE MOD(`uid`, 8)
        WHEN 1 THEN 'https://bkimg.cdn.bcebos.com/pic/5d6034a85edf8db1cb13ede8e869ca54564e9358deae?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 2 THEN 'https://bkimg.cdn.bcebos.com/pic/8601a18b87d6277f9e2f849352610830e924b899565c?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 3 THEN 'https://bkimg.cdn.bcebos.com/pic/810a19d8bc3eb13533faa690d742bfd3fd1f4134eede?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 4 THEN 'https://bkimg.cdn.bcebos.com/pic/9c16fdfaaf51f3deb48fe31d9db7e71f3a292df5d30b?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 5 THEN 'https://bkimg.cdn.bcebos.com/pic/9f2f070828381f30e92493e37f535b086e061d955a64?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 6 THEN 'https://bkimg.cdn.bcebos.com/pic/d52a2834349b033b5bb568908e9721d3d539b600177d?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        WHEN 7 THEN 'https://bkimg.cdn.bcebos.com/pic/a50f4bfbfbedab64034f6efa326fb8c379310a55b0ca?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
        ELSE 'https://bkimg.cdn.bcebos.com/pic/8601a18b87d6277f9e2fb08a6e630830e924b8995677?x-bce-process=image/resize,m_lfit,w_536,limit_1/quality,Q_70'
    END
    ELSE `avatar`
END;

SELECT `gender`, COUNT(*) AS `count`
FROM `user`
GROUP BY `gender`
ORDER BY `gender`;

SELECT `uid`, `username`, `gender`, `avatar`
FROM `user`
ORDER BY `uid`
LIMIT 20;
