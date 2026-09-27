package org.aileme.shejiao.app.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.api.service.SysAreaService;
import org.aileme.shejiao.api.service.SysUniversityService;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.sys.SysArea;
import org.aileme.shejiao.domain.entity.sys.SysUniversity;


import java.util.List;
import java.util.Map;

/**
 * <p>
 * 行政区域地州市信息表 前端控制器
 * </p>
 *
 * @author 小灰灰
 * @since 2023-04-28
 */
@RestController
@RequestMapping("/app/cities")
@Tag(name = "移动端——城市列表")
public class CitiesController {


    @Autowired
    private SysUniversityService sysUniversityService;

    @Operation(summary = "城市信息")
    @GetMapping("/city.json")
    public R getCities() {
//        Map<String, List<SysArea>>  map = provincesMap();
        Map<String, Map<String, List<SysArea>>> cityMap= SysAreaService.cityMap;
        return R.ok().put("data", cityMap);
    }

    @Operation(summary = "根据城市id获取城市信息")
    @GetMapping("/getCityByID.json")
    public R getCityByID(@RequestParam String cityId) {
        Object map = SysAreaService.cityMap.get(cityId);
        return R.ok().put("data", map);
    }

    @GetMapping("/getSchools.json")
    @Operation(summary = "模糊查询大学")
    public Result<List<SysUniversity>> getSchools(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return new Result<List<SysUniversity>>().ok(List.of());
        }
        List<SysUniversity> list = sysUniversityService.lambdaQuery()
            .select(SysUniversity::getUniName, SysUniversity::getTag)
            .likeRight(SysUniversity::getUniName, name.trim())
            .last("LIMIT 20")
            .list();
        return new Result<List<SysUniversity>>().ok(list);
    }
}

