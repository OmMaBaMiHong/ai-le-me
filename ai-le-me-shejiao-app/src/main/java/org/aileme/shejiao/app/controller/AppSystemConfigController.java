/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * <p>
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.aileme.system.domain.bo.SysDictDataBo;
import org.aileme.system.domain.vo.SysDictDataVo;
import org.aileme.system.service.ISysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppTotalSwitchSupport;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author linfeng
 * @date 2022/1/19 16:37
 */
@RestController
@RequestMapping("/app/system")
@Tag(name = "移动端——获取配置")
public class AppSystemConfigController {

    private static final String MINIAPP_FILING_GLOBAL_KEY = Constant.IS_OPEN;
    private static final String MINIAPP_FILING_KEY_PREFIX = "miniapp.filing.";


    @Autowired
    private SysConfigService configService;

    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;

    @Autowired
    private ISysDictDataService dictDataService;


//    @Operation(summary = "用户端——查询app配置信息")
//    @GetMapping("/miniConfig")
//    public Result<SystemEntity> info(){
//        String value = configService.getValue(Constant.IMG);
//        SystemEntity system = new SystemEntity();
//        system.setIntro(value);
//        return new Result<SystemEntity>().ok(system);
//    }


    @Operation(summary = "用户端——登录注册页面配置信息")
    @GetMapping("/config")
    public R config() {
        String logoUrl = configService.getValue(Constant.IMG);
        String emailLogin = configService.getValue(Constant.EMAIL_LOGIN);

        return R.ok().put("logoUrl", logoUrl).put("emailLogin", emailLogin);
    }


    @Operation(summary = "用户端——用户页面背景图")
    @GetMapping("/bgImgConfig")
    public R bgImgConfig() {
        String bgImg = configService.getValue(Constant.BG_IMG);

        return R.ok().put("bgImg", bgImg);
    }


    @Operation(summary = "用户端——查询用户服务协议")
    @GetMapping("/protocol")
    public R protocol() {
        String value = configService.getValue(Constant.PROTOCOL);
        return R.ok().put("result", value);
    }

    @Operation(summary = "用户端——查询个人隐私协议")
    @GetMapping("/privacy")
    public R privacy() {
        String value = configService.getValue(Constant.PRIVACY);
        return R.ok().put("result", value);
    }


    @Operation(summary = "用户端——查询会员充值协议")
    @GetMapping("/vipRecharge")
    public R vipRecharge() {
        String value = configService.getValue(Constant.VIP_AGREE_CONTENT);
        return R.ok().put("result", value);
    }

    @Operation(summary = "用户端——查询职业列表")
    @GetMapping("/getJobs")
    public R getJobs() {
        return getDictData("sys_job_type");
    }

    @Operation(summary = "用户端——查询身高列表")
    @GetMapping("/getHeigjt")
    public R getHeigjt() {
        String value = configService.getValue("height");
        return R.ok().put("result", value);
    }

    /**
     * 通用字典数据查询接口
     * 支持查询性别、婚姻状况、学历、年薪等所有字典数据
     * 
     * @param dictType 字典类型，如：sys_user_sex、marry_status、education_level、income_range
     * @return 字典数据列表 [{id, value, label}]
     */
    @Operation(summary = "用户端——通用字典数据查询", 
               description = "查询指定类型的字典数据，返回格式：[{id, value, label}]")
    @Parameter(name = "dictType", description = "字典类型：sys_user_sex(性别)、marry_status(婚姻)、education_level(学历)、income_range(年薪)、sys_job_type(职业)", required = true)
    @GetMapping("/getDictData")
    public R getDictData(@RequestParam String dictType) {
        SysDictDataBo bo = new SysDictDataBo();
        bo.setDictType(dictType);
        List<SysDictDataVo> dictList = dictDataService.selectDictDataList(bo);
        
        // 转换为前端需要的格式：[{id, value, label}]
        List<Map<String, Object>> result = dictList.stream()
            .map(dict -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", dict.getDictCode());
                map.put("value", dict.getDictValue());
                map.put("label", dict.getDictLabel());
                return map;
            })
            .collect(Collectors.toList());
        
        return R.ok().put("result", result);
    }

    /**
     * 批量查询多个字典数据
     * 一次请求获取多个字典类型的数据，减少网络请求
     * 
     * @param dictTypes 字典类型列表，逗号分隔，如：sys_user_sex,marry_status,education_level
     * @return Map结构，key为字典类型，value为对应的字典数据列表
     */
    @Operation(summary = "用户端——批量查询字典数据", 
               description = "一次性查询多个字典类型的数据，返回格式：{dictType: [{id, value, label}]}")
    @Parameter(name = "dictTypes", description = "字典类型列表，逗号分隔，如：sys_user_sex,marry_status,education_level", required = true)
    @GetMapping("/getBatchDictData")
    public R getBatchDictData(@RequestParam String dictTypes) {
        String[] typeArray = dictTypes.split(",");
        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();
        
        for (String dictType : typeArray) {
            dictType = dictType.trim();
            if (dictType.isEmpty()) {
                continue;
            }
            
            SysDictDataBo bo = new SysDictDataBo();
            bo.setDictType(dictType);
            List<SysDictDataVo> dictList = dictDataService.selectDictDataList(bo);
            
            List<Map<String, Object>> list = dictList.stream()
                .map(dict -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", dict.getDictCode());
                    map.put("value", dict.getDictValue());
                    map.put("label", dict.getDictLabel());
                    return map;
                })
                .collect(Collectors.toList());
            
            resultMap.put(dictType, list);
        }
        
        return R.ok().put("result", resultMap);
    }

    @Operation(summary = "用户端——获取配置信息")
    @GetMapping("/getSysConfig")
    public R getSysConfig(@RequestParam String key) {
        String normalizedKey = normalizeConfigKey(key);
        String value = configService.getValue(normalizedKey);
        if (StringUtils.equals(normalizedKey, MINIAPP_FILING_GLOBAL_KEY)) {
            value = MiniAppTotalSwitchSupport.normalizeValue(value, true);
        }
        return R.ok().put("result", value);
    }

    private String normalizeConfigKey(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        }
        if (StringUtils.equals(key, Constant.IS_OPEN)) {
            return Constant.IS_OPEN;
        }
        if (StringUtils.startsWith(key, MINIAPP_FILING_KEY_PREFIX)) {
            return Constant.IS_OPEN;
        }
        return key;
    }

    @Operation(summary = "用户端——查询app基础配置信息")
    @GetMapping("/basic")
    public R basic() {
        String logo = configService.getValue(Constant.IMG);
        String indexStyle1 = configService.getValue(Constant.INDEX_STYLE_LAST);
        String indexStyle2 = configService.getValue(Constant.INDEX_STYLE_TOPIC);
        String indexStyle3 = configService.getValue(Constant.INDEX_STYLE_WATCH);
        String vipShow = configService.getValue(Constant.VIP_SHOW);
        String iosClose = configService.getValue(Constant.IOS_CLOSE);

        return R.ok()
                .put("logo", logo)
                .put("indexStyle1", indexStyle1)
                .put("indexStyle2", indexStyle2)
                .put("indexStyle3", indexStyle3)
                .put("iosClose", iosClose)
                .put("vipShow", vipShow);
    }

    @Operation(summary = "用户端——查询app登录基础配置信息")
    @GetMapping("/loginBasic")
    public R loginBasic() {
        String logo = configService.getValue(Constant.IMG);
        String smsOpen = configService.getValue(Constant.SMS_OPEN);
        return R.ok()
                .put("logo", logo)
                .put("smsOpen", smsOpen);
    }


    @Operation(summary = "用户端——查询vip开关配置信息")
    @GetMapping("/vipShow")
    public R vipShow() {
        String vipShow = configService.getValue(Constant.VIP_SHOW);

        return R.ok().put("vipShow", vipShow);
    }


    @Operation(summary = "用户端——查询流量主广告基本设置")
    @GetMapping("/getAd")
    public R getAd() {
        String adIsOpen = configService.getValue(Constant.AD_IS_OPEN);
        String wxAdpid = wechatRuntimeConfigService.getMiniAdPid();
        String h5Adpid = configService.getValue(Constant.H5_AD_PID);
        return R.ok()
                .put("wxAdpid", wxAdpid)
                .put("adIsOpen", adIsOpen)
                .put("h5Adpid", h5Adpid);
    }


    @Operation(summary = "用户端——查询首页弹窗广告设置")
    @GetMapping("/getPop")
    public R getPop() {
        String popupOpen = configService.getValue(Constant.POPUP_OPEN);
        if ("1".equals(popupOpen)) {
            return R.ok().put("popupOpen", popupOpen);
        }
        String popTitle = configService.getValue(Constant.POP_TITLE);
        String popContent = configService.getValue(Constant.POP_CONTENT);
        String popTime = configService.getValue(Constant.POP_TIME);
        return R.ok()
                .put("popupOpen", popupOpen)
                .put("popTitle", popTitle)
                .put("popContent", popContent)
                .put("popTime", popTime);
    }


    @Login
    @Operation(summary = "用户端——查询付费贴开关")
    @GetMapping("/checkPayPostBtn")
    public R checkPayPostBtn(@Parameter(hidden = true) @LoginUser AppUserEntity user) {
        boolean isOpen = true;
        String payPostBtn = configService.getValue(Constant.PAY_POST_BTN);
        String isVipOpen = configService.getValue(Constant.PAY_POST_VIP);
        if (payPostBtn.equals("1")) {
            isOpen = false;
        }
        if (isVipOpen.equals("1") && user.getVip().equals(Constant.COMMON_USER)) {
            isOpen = false;
        }
        return R.ok()
                .put("isOpen", isOpen);
    }

}
