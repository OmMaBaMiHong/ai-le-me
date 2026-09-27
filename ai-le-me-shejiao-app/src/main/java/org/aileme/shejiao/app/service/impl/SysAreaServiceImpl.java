package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SysAreaService;
import org.aileme.shejiao.app.dao.SysAreaDao;
import org.aileme.shejiao.domain.entity.sys.SysArea;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DS("master")
@Service
public class SysAreaServiceImpl extends ServiceImpl<SysAreaDao, SysArea> implements SysAreaService, CommandLineRunner {
    @Autowired
    private SysAreaDao sysAreaDao;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SysAreaServiceImpl.class);

    @Override
    public void run(String... args) {
        try {
            List<SysArea> areaList = this.list();

            // ========== 步骤1：提取省级/市级映射关系 ==========
            // 1.1 省级数据：level=1，parentId=0 → 映射：省级id → 省级信息（areaCode/name/zipCode）
            Map<Integer, SysArea> provinceIdMap = areaList.stream().filter(area -> area.getLevel() == 1).collect(Collectors.toMap(SysArea::getCode, area -> area));
            //省 id-》市区
            Map<Integer, List<SysArea>> provinceIdCitys = areaList.stream().filter(area -> area.getLevel() == 2).collect(Collectors.groupingBy(SysArea::getPcode));
            //市区id=>区列表
            Map<Integer, List<SysArea>> cityIdArea = areaList.stream().filter(area -> area.getLevel() == 3).collect(Collectors.groupingBy(SysArea::getPcode));
            Map<String, Map<String, List<SysArea>>> result = new HashMap<>();
            provinceIdMap.forEach((provinceId, province) -> {
                List<SysArea> cites = provinceIdCitys.get(provinceId);
                Map<String, List<SysArea>> cityMap = new HashMap<>();
                if (cites != null){
                    cites.forEach(city -> {
                        List<SysArea> areas = cityIdArea.get(city.getCode());
                        cityMap.put(city.getName(), areas);
                    });
                }
                result.put(province.getName(), cityMap);
            });
            cityMap.putAll(result);

            log.info("行政区域数据初始化成功");
        } catch (Exception e) {
            log.error("行政区域数据初始化失败，请检查数据库表 provinces, cities, areas 是否存在: {}", e.getMessage());
        }
    }
}
