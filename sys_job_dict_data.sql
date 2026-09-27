-- 职业字典类型（如果你已经在后台创建了 sys_job_type，这部分可以跳过）
-- INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, create_by, create_time, remark)
-- VALUES (100, '职业类型', 'sys_job_type', 1, NOW(), '用户职业分类字典');

-- 职业字典数据（适合婚恋场景，共53个常见职业 + 分类标识）
-- 说明：dict_value 用数字编码便于后续统计分析，dict_label 是用户看到的中文名称
-- dict_sort 数字越小越靠前，热门职业排在前面

DELETE FROM sys_dict_data WHERE dict_type = 'sys_job_type';

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, remark, create_by, create_time) VALUES
-- 互联网/IT（编码 1xx）
('sys_job_type', '产品经理', '101', 1, '互联网/IT', 1, NOW()),
('sys_job_type', '软件工程师', '102', 2, '互联网/IT', 1, NOW()),
('sys_job_type', 'UI设计师', '103', 3, '互联网/IT', 1, NOW()),
('sys_job_type', '运营专员', '104', 4, '互联网/IT', 1, NOW()),
('sys_job_type', '数据分析师', '105', 5, '互联网/IT', 1, NOW()),
('sys_job_type', '测试工程师', '106', 6, '互联网/IT', 1, NOW()),
('sys_job_type', '项目经理', '107', 7, '互联网/IT', 1, NOW()),

-- 金融/财会（编码 2xx）
('sys_job_type', '会计', '201', 8, '金融/财会', 1, NOW()),
('sys_job_type', '金融分析师', '202', 9, '金融/财会', 1, NOW()),
('sys_job_type', '投资顾问', '203', 10, '金融/财会', 1, NOW()),
('sys_job_type', '银行职员', '204', 11, '金融/财会', 1, NOW()),
('sys_job_type', '审计师', '205', 12, '金融/财会', 1, NOW()),

-- 医疗/健康（编码 3xx）
('sys_job_type', '医生', '301', 13, '医疗/健康', 1, NOW()),
('sys_job_type', '护士', '302', 14, '医疗/健康', 1, NOW()),
('sys_job_type', '药剂师', '303', 15, '医疗/健康', 1, NOW()),
('sys_job_type', '医疗器械销售', '304', 16, '医疗/健康', 1, NOW()),

-- 教育/培训（编码 4xx）
('sys_job_type', '教师', '401', 17, '教育/培训', 1, NOW()),
('sys_job_type', '培训师', '402', 18, '教育/培训', 1, NOW()),
('sys_job_type', '教育顾问', '403', 19, '教育/培训', 1, NOW()),

-- 公务员/事业单位（编码 5xx，婚恋市场热门）
('sys_job_type', '公务员', '501', 20, '公务员/事业单位', 1, NOW()),
('sys_job_type', '事业单位', '502', 21, '公务员/事业单位', 1, NOW()),

-- 销售/市场（编码 6xx）
('sys_job_type', '销售经理', '601', 22, '销售/市场', 1, NOW()),
('sys_job_type', '市场专员', '602', 23, '销售/市场', 1, NOW()),
('sys_job_type', '客户经理', '603', 24, '销售/市场', 1, NOW()),

-- 设计/创意（编码 7xx）
('sys_job_type', '平面设计师', '701', 25, '设计/创意', 1, NOW()),
('sys_job_type', '室内设计师', '702', 26, '设计/创意', 1, NOW()),
('sys_job_type', '服装设计师', '703', 27, '设计/创意', 1, NOW()),
('sys_job_type', '建筑设计师', '704', 28, '设计/创意', 1, NOW()),

-- 传媒/文化（编码 8xx）
('sys_job_type', '记者/编辑', '801', 29, '传媒/文化', 1, NOW()),
('sys_job_type', '摄影师', '802', 30, '传媒/文化', 1, NOW()),
('sys_job_type', '主持人', '803', 31, '传媒/文化', 1, NOW()),
('sys_job_type', '导演/编剧', '804', 32, '传媒/文化', 1, NOW()),

-- 律师/法务（编码 9xx）
('sys_job_type', '律师', '901', 33, '律师/法务', 1, NOW()),
('sys_job_type', '法务专员', '902', 34, '律师/法务', 1, NOW()),

-- 制造/工程（编码 10xx）
('sys_job_type', '机械工程师', '1001', 35, '制造/工程', 1, NOW()),
('sys_job_type', '电气工程师', '1002', 36, '制造/工程', 1, NOW()),
('sys_job_type', '生产主管', '1003', 37, '制造/工程', 1, NOW()),

-- 建筑/房地产（编码 11xx）
('sys_job_type', '建筑师', '1101', 38, '建筑/房地产', 1, NOW()),
('sys_job_type', '房地产经纪人', '1102', 39, '建筑/房地产', 1, NOW()),
('sys_job_type', '造价工程师', '1103', 40, '建筑/房地产', 1, NOW()),

-- 服务业（编码 12xx）
('sys_job_type', '餐饮服务', '1201', 41, '服务业', 1, NOW()),
('sys_job_type', '酒店管理', '1202', 42, '服务业', 1, NOW()),
('sys_job_type', '美容美发', '1203', 43, '服务业', 1, NOW()),
('sys_job_type', '健身教练', '1204', 44, '服务业', 1, NOW()),

-- 物流/运输（编码 13xx）
('sys_job_type', '物流专员', '1301', 45, '物流/运输', 1, NOW()),
('sys_job_type', '司机', '1302', 46, '物流/运输', 1, NOW()),

-- 人力资源/行政（编码 14xx）
('sys_job_type', '人力资源', '1401', 47, '人力资源/行政', 1, NOW()),
('sys_job_type', '行政专员', '1402', 48, '人力资源/行政', 1, NOW()),

-- 自由职业/其他（编码 15xx）
('sys_job_type', '自由职业', '1501', 49, '自由职业/其他', 1, NOW()),
('sys_job_type', '个体经营', '1502', 50, '自由职业/其他', 1, NOW()),
('sys_job_type', '学生', '1503', 51, '自由职业/其他', 1, NOW()),
('sys_job_type', '暂无工作', '1504', 52, '自由职业/其他', 1, NOW()),
('sys_job_type', '其他', '1599', 99, '自由职业/其他', 1, NOW());

-- 查询验证
SELECT dict_label AS '职业名称', dict_value AS '编码', remark AS '分类' 
FROM sys_dict_data 
WHERE dict_type = 'sys_job_type' 
ORDER BY dict_sort;
