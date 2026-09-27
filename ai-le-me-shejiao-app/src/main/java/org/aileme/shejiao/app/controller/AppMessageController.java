/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * 
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.MessageNumberResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.MessageService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.MessageReadForm;
import org.aileme.shejiao.domain.param.app.UpdateChatStatusForm;
import org.aileme.shejiao.domain.param.app.UpdateSystemNoticeStatusForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 
 *  APP 系统消息模块
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-26 13:15:30
 */
@RestController
@RequestMapping("/app/message")
@Tag(name = "移动端——系统消息模块")
public class AppMessageController {


    @Autowired
    private MessageService messageService;

    /**
     * 消息列表分页
     */
    @Login
    @GetMapping("/list")
    @Operation(summary = "消息列表分页")
    @Parameters({
            @Parameter(name = "type", description = "类型", required = true),
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<AppPageUtils> list(@RequestParam("type") Integer type,
                                     @RequestParam("page") Integer page,
                                     @Parameter(hidden = true) @LoginUser AppUserEntity user){
        AppPageUtils pages=messageService.queryMessageList(type,page,user);

        return new Result<AppPageUtils>().ok(pages);
    }


    /**
     * 消息数量
     */
    @PostMapping("/num")
    @Operation(summary = "消息数量")
    public Result<MessageNumberResponse> num(){
        MessageNumberResponse response=messageService.getMessageNumber();

        return new Result<MessageNumberResponse>().ok(response);
    }


    @Login
    @GetMapping("/status")
    @Operation(summary = "设置消息已读")
    @Parameters({
            @Parameter(name = "type", description = "类型", required = true)
    })
    public Result status(@RequestParam("type") Integer type,
                    @Parameter(hidden = true) @LoginUser AppUserEntity user){
        messageService.status(type, user.getUid());
        return new Result();
    }



    @Login
    @PostMapping("/readMessage")
    @Operation(summary = "设置用户点击的消息为已读")
    public Result readMessage(@RequestBody MessageReadForm request,
                         @Parameter(hidden = true) @LoginUser AppUserEntity user){
        messageService.readMessage(request, user.getUid());
        return new Result();
    }

    @Login
    @GetMapping("/readAllWatchInfo")
    @Operation(summary = "设置所有用户关注消息为已读")
    public Result readAllWatchInfo(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        messageService.readAllWatchInfo(user.getUid());
        return new Result();
    }

    @Login
    @PostMapping("/articleMsgState")
    @Operation(summary = "设置系统消息为已读")
    public Result articleMsgState(@RequestBody UpdateSystemNoticeStatusForm request,
                             @Parameter(hidden = true) @LoginUser AppUserEntity user){
        Boolean status = messageService.articleMsgState(request, user.getUid());
        if(status){
            return new Result();
        }
        return new Result().error();
    }

    @Login
    @PostMapping("/updateSystemStatus")
    @Operation(summary = "设置所有系统消息为已读")
    public Result updateSystemStatus(@RequestBody UpdateChatStatusForm request,@Parameter(hidden = true)  @LoginUser AppUserEntity user){
        messageService.updateSystemStatus(request, user.getUid());
        return new Result();
    }

    @Login
    @PostMapping("/delSystemMsg")
    @Operation(summary = "用户删除系统消息")
    public Result delSystemMsg(@Parameter(hidden = true)  @LoginUser AppUserEntity user){
        messageService.delSystemMsg(user.getUid());
        return new Result();
    }

    @Login
    @PostMapping("/deleteById")
    @Operation(summary = "删除单条互动消息")
    public Result deleteById(@RequestBody MessageReadForm request,
                             @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (request == null || request.getMid() == null) {
            return new Result().error("消息ID不能为空");
        }
        boolean deleted = messageService.deleteMessageById(request.getMid(), user.getUid());
        if (!deleted) {
            return new Result().error("消息不存在或已删除");
        }
        return new Result().ok();
    }
}
