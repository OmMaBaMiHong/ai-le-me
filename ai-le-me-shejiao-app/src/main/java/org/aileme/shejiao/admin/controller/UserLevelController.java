package org.aileme.shejiao.admin.controller;

import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.ConfigConstant;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.UserLevelEntity;
import org.aileme.shejiao.api.service.UserLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 用户经验值设置
 *
 * @author Wade
 * @date 2026-02-10
 */
@RestController
@RequestMapping("/admin/userlevel")
@Tag(name = "管理端——用户经验值设置管理")
public class UserLevelController {

    @Autowired
    private UserLevelService userLevelService;

    /**
     * 列表
     */
    @GetMapping("/list")
    @Operation(summary = "用户经验值列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = userLevelService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    @Operation(summary = "用户经验值信息")
    public R info(@PathVariable("id") Integer id){
        UserLevelEntity userLevel = userLevelService.getById(id);

        return R.ok().put("userLevel", userLevel);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    @Operation(summary = "用户经验值保存")
    public R save(@RequestBody UserLevelEntity userLevel){
        // 验证等级ID范围
        if(userLevel.getLevelId() == null || userLevel.getLevelId() < 1 || userLevel.getLevelId() > 5){
            throw new LinfengException("用户等级ID需要在1到5之间");
        }
        
        // 清除Redis缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.USER_LEVEL_KEY);
        
        userLevelService.save(userLevel);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @Operation(summary = "用户经验值修改")
    public R update(@RequestBody UserLevelEntity userLevel){
        // 验证等级ID范围
        if(userLevel.getLevelId() == null || userLevel.getLevelId() < 1 || userLevel.getLevelId() > 5){
            throw new LinfengException("用户等级ID需要在1到5之间");
        }
        
        // 清除Redis缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.USER_LEVEL_KEY);
        
        userLevelService.updateById(userLevel);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @Operation(summary = "用户经验值删除")
    public R delete(@RequestBody Integer[] ids){
        // 清除Redis缓存
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.USER_LEVEL_KEY);
        
        userLevelService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
