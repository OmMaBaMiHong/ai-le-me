package org.aileme.shejiao.app.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import org.aileme.shejiao.app.oss.ImageUtil;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.entity.app.UserScans;
import org.aileme.shejiao.domain.param.app.FollowEnums;
import org.aileme.shejiao.api.service.FollowService;
import org.aileme.shejiao.api.service.UserScansService;

import java.time.*;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 个人主页浏览记录 前端控制器
 * </p>
 *
 * @author lww
 * @since 2023-05-16
 */
@Tag(name = "移动端——用户主页浏览记录")
@RestController
@RequestMapping("/app/userScans")
public class UserScansController {
    @Autowired
    private UserScansService userScansService;
    @Autowired
    private FollowService followService;

    @Login
    @PostMapping("/doScan")
    @Operation(summary = "浏览个人主页埋点")
    public R doScan (@Parameter(hidden = true) @LoginUser AppUserEntity user, @RequestParam int scanUid){
        QueryWrapper<UserScans> queryWrapper= new QueryWrapper<>();
        queryWrapper.lambda().eq(UserScans::getScanUid,user.getUid()).eq(UserScans::getUid,scanUid);
        UserScans userScans= userScansService.getOne(queryWrapper);
        if(userScans==null){
            userScans=new UserScans();
            userScans.setUid(scanUid);
            userScans.setScanUid(user.getUid());
            userScans.setScanNums(1);
            //头像模糊化处理：如果浏览者未关注，非互相喜欢
            Integer f= followService.isFollow(scanUid,user.getUid());
            if(f.equals(FollowEnums.MUTUAL_FOLLOW.getCode())){
                userScans.setLookerAvatar(user.getAvatar());
            }else {
              String mohu=  ImageUtil.mohu(user.getAvatar());
              userScans.setLookerAvatar(mohu);
            }
            userScans.setCreateTime(new Date());
            userScansService.save(userScans);
        }else{
            userScansService.updateScaNums(userScans.getId());
        }
        return R.ok();
    }


    @Login
    @GetMapping("/getScans")
    @Operation(summary = "个人主页浏览记录")
    public R getScans (@Parameter(hidden = true) @LoginUser AppUserEntity user){
        QueryWrapper<UserScans> queryWrapper= new QueryWrapper<>();
        queryWrapper.lambda().eq(UserScans::getUid,user.getUid());
        List<UserScans> userScans= userScansService.list(queryWrapper);
        R r=R.ok();
        r.put("userScans",userScans);
        if(userScans != null && !userScans.isEmpty()){
            //因为LocalDate不包含时间，所以转Date时，会默认转为当天的起始时间，00:00:00
            LocalDate localDate4 = LocalDate.now(ZoneId.systemDefault());
            LocalDateTime todayDate = LocalDateTime.of(localDate4, LocalTime.of(0,0));
            //今日浏览量
            long dayScans=  userScans.stream().filter(f-> LocalDateTime.ofInstant(f.getCreateTime().toInstant(), ZoneId.systemDefault()).isAfter(todayDate)).count();
            //反复查看
            long overScans=  userScans.stream().filter(f-> f.getScanNums()>1).count();
            r.put("scans",userScans.size());
            r.put("dayScans",dayScans);
            r.put("overScans",overScans);
        }
        return r;
    }
}

