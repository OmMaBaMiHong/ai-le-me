/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.ClearChatMessageUnreadForm;
import org.aileme.shejiao.domain.param.app.GetChatListForm;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linfeng
 * @date 2022/11/16 16:55
 */
@RestController
@RequestMapping("/app/chat")
@Tag(name = "移动端——私聊")
public class AppChatMessageController {


    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private FriendService friendService;
    /**
     * IM会话列表+缓存
     * @param param
     * @return
     */
    @Login
    @PostMapping("/list")
    @Operation(summary = "获取私聊分页列表")
    public R getChatMessageList(@RequestBody GetChatListForm param){
        IPage<ChatMessageEntity> chatMessageList = chatMessageService.getChatMessageList(param);
        Map<String,Object> map=new HashMap<>();
        map.put("sessionId",param.getSessionId());
        map.put("pageInfo",chatMessageList);
        return R.ok().put("data",map);
    }

    @PostMapping("/clearUnread")
    @Operation(summary = "清理私聊未读数据")
    public R clearUnread(@RequestBody ClearChatMessageUnreadForm param) {
        friendService.clearUnread(param);
        return R.ok();
    }

    @Login
    @PostMapping("/deleteSession")
    @Operation(summary = "删除聊天会话")
    public R deleteSession(@RequestBody Map<String, Object> params) {
        String sessionId = params.get("sessionId").toString();
        chatMessageService.deleteBySessionId(sessionId);
        friendService.deleteBySessionId(sessionId);
        return R.ok();
    }

    @Login
    @PostMapping("/getOrCreateSession")
    @Operation(summary = "获取或创建聊天会话（透明创建，无需好友关系）")
    public R getOrCreateSession(@RequestBody Map<String, Object> params,
                                @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer targetUid = Integer.valueOf(params.get("targetUid").toString());
        Long sessionId = friendService.getOrCreateSession(user.getUid(), targetUid);
        return R.ok().put("sessionId", String.valueOf(sessionId));
    }

}
