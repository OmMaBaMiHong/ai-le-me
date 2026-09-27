package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.ReadNoticeForm;
import org.aileme.shejiao.domain.param.app.RejectNoticeForm;
import org.aileme.shejiao.api.service.NoticeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author linfeng
 * @date 2022/11/16 16:55
 */
@RestController
@RequestMapping("/app/notice")
@Tag(name = "移动端——IM通知")
public class AppNoticeController {

    @Autowired
    private NoticeService noticeService;


    @Login
    @GetMapping("/list")
    @Operation(summary = "获取IM通知列表")
    public R getNoticeList(@LoginUser AppUserEntity user){
        Map<String, Object> map = noticeService.getNoticeList(user.getUid());
        return R.ok().put("data",map);
    }

    @Login
    @PostMapping("/readById")
    @Operation(summary = "根据id已读消息")
    public R readById(@RequestBody ReadNoticeForm param){
        noticeService.readById(param);
        return R.ok();
    }

    @Login
    @PostMapping("/reject")
    @Operation(summary = "拒绝好友申请")
    public R reject(@RequestBody RejectNoticeForm param){
        noticeService.reject(param);
        return R.ok();
    }

    @Login
    @PostMapping("/delete")
    @Operation(summary = "删除好友申请")
    public R delete(@RequestBody ReadNoticeForm param){
        noticeService.removeById(param.getId());
        return R.ok();
    }

}
