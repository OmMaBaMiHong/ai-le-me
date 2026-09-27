/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import cn.hutool.json.JSONObject;
import org.springframework.web.bind.annotation.RequestParam;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.domain.param.app.DeleteFriendForm;
import org.aileme.shejiao.domain.param.app.ApplyFriendForm;
import org.aileme.shejiao.domain.param.app.AgreePersonForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author linfeng
 * @date 2022/11/16 16:55
 */
@RestController
@RequestMapping("/app/friend")
@Tag(name = "移动端——好友")
public class AppFriendController {

    @Autowired
    private FriendService friendService;


    @Login
    @GetMapping("/list")
    @Operation(summary = "获取好友列表")
    public R getFriendList(@LoginUser AppUserEntity user){
        List<JSONObject> list=friendService.getFriendList(user.getUid());
        return R.ok().put("data",list);
    }
    @Login
    @GetMapping("/getFriend")
    @Operation(summary = "获取好友聊天信息")
    public R getFriend(@LoginUser AppUserEntity user, @RequestParam Integer firendId){
        FriendEntity friendEntity= friendService.lambdaQuery().eq(FriendEntity::getMyId,user.getUid()).eq(FriendEntity::getFriendId,firendId).one();
        return R.ok().put("data",friendEntity);
    }

    @Login
    @PostMapping("/deleteFriend")
    @Operation(summary = "删除指定好友")
    public R deleteFriend(@LoginUser AppUserEntity user,
                          @RequestBody DeleteFriendForm param){
        friendService.deleteFriend(param.getId(), user.getUid());
        return R.ok();
    }

    /**
     * websocket 掉线状态下的申请好友方式
     */
    @Login
    @PostMapping("/applyFriend")
    @Operation(summary = "申请好友")
    public R applyFriend(@RequestBody ApplyFriendForm param){
        friendService.applyFriend(param.getData());
        return R.ok();
    }

    /**
     * websocket 掉线状态下的同意申请好友方式
     */
    @Login
    @PostMapping("/agreePersonApply")
    @Operation(summary = "同意申请好友")
    public R agreePersonApply(@RequestBody AgreePersonForm param){
        friendService.agreePersonApply(param.getId());
        return R.ok();
    }
}
