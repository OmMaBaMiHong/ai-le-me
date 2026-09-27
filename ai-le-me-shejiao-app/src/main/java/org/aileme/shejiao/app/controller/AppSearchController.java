package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.SearchService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.entity.app.SearchEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linfeng
 * @date 2023/4/26 20:26
 */
@Tag(name = "移动端——用户历史搜索")
@RestController
@RequestMapping("app/search")
public class AppSearchController {

    @Autowired
    private SearchService searchService;


    @GetMapping("/getUserSearchHistory")
    @Operation(summary = "获取用户搜索历史列表")
    public R getUserSearchHistory(){
        List<SearchEntity> list=new ArrayList<>();
        // 实际项目中应该从登录上下文获取用户
        // 这里暂时返回空列表
        return R.ok().put("result",list);
    }


    @GetMapping("/getHotSearchHistory")
    @Operation(summary = "获取热门搜索历史列表")
    public R getHotSearchHistory(){
        List<String> list=searchService.selectHotSearch();
        return R.ok().put("result",list);
    }

    @PostMapping("/deleteSearchById/{id}")
    @Operation(summary = "根据 searchId 删除搜索历史")
    public R deleteSearchById(@PathVariable Long id){
        searchService.removeById(id);
        return R.ok();
    }


    @Login
    @PostMapping("/deleteSearchByUId")
    @Operation(summary = "删除该用户全部搜索历史")
    public R deleteSearchByUId(@LoginUser AppUserEntity user){
        searchService.deleteSearchByUId(user.getUid());
        return R.ok();
    }

}
