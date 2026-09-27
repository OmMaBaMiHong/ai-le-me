SET NAMES utf8mb4;

SET @quick_entry_now = NOW(6);

UPDATE navigation
SET title = '性格测试',
    img = COALESCE(NULLIF(img, ''), ''),
    url = '/subpackages/tools/personality',
    type = 0,
    status = 0,
    update_time = @quick_entry_now
WHERE title = '性格测试'
   OR url = '/subpackages/tools/personality';

INSERT INTO navigation (create_time, img, status, title, type, update_time, url)
SELECT @quick_entry_now, '', 0, '性格测试', 0, @quick_entry_now, '/subpackages/tools/personality'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM navigation
    WHERE title = '性格测试'
       OR url = '/subpackages/tools/personality'
);

UPDATE navigation
SET title = '星象占卜',
    img = COALESCE(NULLIF(img, ''), ''),
    url = '/subpackages/tools/constellation',
    type = 0,
    status = 0,
    update_time = @quick_entry_now
WHERE title = '星象占卜'
   OR url = '/subpackages/tools/constellation';

INSERT INTO navigation (create_time, img, status, title, type, update_time, url)
SELECT @quick_entry_now, '', 0, '星象占卜', 0, @quick_entry_now, '/subpackages/tools/constellation'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM navigation
    WHERE title = '星象占卜'
       OR url = '/subpackages/tools/constellation'
);

UPDATE navigation
SET title = '老黄历',
    img = COALESCE(NULLIF(img, ''), ''),
    url = '/subpackages/tools/almanac',
    type = 0,
    status = 0,
    update_time = @quick_entry_now
WHERE title = '老黄历'
   OR url = '/subpackages/tools/almanac';

INSERT INTO navigation (create_time, img, status, title, type, update_time, url)
SELECT @quick_entry_now, '', 0, '老黄历', 0, @quick_entry_now, '/subpackages/tools/almanac'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM navigation
    WHERE title = '老黄历'
       OR url = '/subpackages/tools/almanac'
);

SELECT id, title, url, type, status
FROM navigation
WHERE title IN ('性格测试', '星象占卜', '老黄历')
   OR url IN (
       '/subpackages/tools/personality',
       '/subpackages/tools/constellation',
       '/subpackages/tools/almanac'
   )
ORDER BY id;
