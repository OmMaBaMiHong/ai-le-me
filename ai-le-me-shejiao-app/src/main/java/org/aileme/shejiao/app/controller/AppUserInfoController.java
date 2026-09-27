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

import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.common.annotation.Limit;
import org.aileme.shejiao.common.annotation.NoRepeatSubmit;
import org.aileme.shejiao.common.enums.EduEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.common.validator.ValidatorUtils;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.RechargeService;
import org.aileme.shejiao.api.service.UserSettingService;
import org.aileme.shejiao.api.service.UserImpressionTagService;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.MessageEntity;
import org.aileme.shejiao.domain.entity.admin.RechargeEntity;
import org.aileme.shejiao.domain.entity.admin.UserInfo;
import org.aileme.shejiao.domain.entity.admin.VipOptionEntity;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;
import org.aileme.shejiao.domain.vo.*;

import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppTotalSwitchSupport;
import org.aileme.shejiao.app.service.PersonaPreGenerationService;
import org.aileme.shejiao.app.utils.WechatUtil;
import org.aileme.shejiao.app.utils.EducationAuthApi;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;
import org.aileme.shejiao.api.service.ProfileVisitService;
import org.aileme.shejiao.api.service.SmsSenderService;
import org.aileme.shejiao.common.utils.JwtUtils;
import org.aileme.shejiao.common.utils.weixin.sdk.IDRealNameAPi;
import org.aileme.shejiao.domain.param.app.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.*;

/**
 * 用户登录授权
 *
 */
@Slf4j
@RestController
@RequestMapping("/app/user")
@Tag(name = "移动端——用户登录")
public class AppUserInfoController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private SmsSenderService smsSenderService;
    @Autowired
    private SysConfigService configService;

    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;

    @Autowired(required = false)
    JavaMailSenderImpl mailSender;

    @Autowired
    private UserSettingService userSettingService;

    @Value("${shejiao.linfeng.email.send}")
    private String send;

    @Autowired
    private UserImpressionTagService userImpressionTagService;

    @Autowired
    private TagsService tagsService;

    @Autowired
    private ProfileVisitService profileVisitService;

    @Autowired
    private PersonaPreGenerationService personaPreGenerationService;

    /**
     * 微信小程序登录
     * 该接口用于已绑定过手机号的账户登录
     */
    @PostMapping("/miniWxLogin")
    @Operation(summary = "登录")
    public R miniWxLogin(@RequestBody WxLoginForm form, HttpServletRequest request){
        //表单校验
        ValidatorUtils.validateEntity(form);

        //用户登录
        Integer userId = appUserService.wxLogin(form,request);
        if(userId==0){
            return R.error(999,"未绑定手机号");
        }
        // 使用 Sa-Token 登录并生成 token
        StpUtil.login(userId, new SaLoginParameter()
            .setTimeout(jwtUtils.getAccessTokenValiditySeconds())
            .setExtra("clientid", "app"));
        String token = StpUtil.getTokenValue();

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("expire", jwtUtils.getAccessTokenValiditySeconds());

        return R.ok(map);
    }

    /**
     * 发送验证码
     * 可配置是否开启验证码
     */
    @Limit(count = 2)
    @PostMapping("/sendSmsCode")
    @Operation(summary = "发送验证码")
    public R sendSmsCode(@RequestBody SendCodeForm request){

        String code=appUserService.sendSmsCode(request);
        String smsOpen = configService.getValue(Constant.SMS_OPEN);
        if("0".equals(smsOpen)){
            //发送阿里云短信
            try {
                smsSenderService.sendLoginCode(request.getMobile(), code);
            } catch (Exception e) {
                String codeKey = "code_" + request.getMobile();
                org.aileme.common.redis.utils.RedisUtils.deleteObject(codeKey);
                e.printStackTrace();
            }
            return R.ok("发送成功,注意查收");
        }
        return R.ok("测试阶段验证码:"+code);
    }

    /**
     * h5登录发送验证码 先判断该手机号是否注册
     * 可配置是否开启验证码
     */
    @Limit(count = 2)
    @PostMapping("/sendLoginSmsCode")
    @Operation(summary = "发送验证码判断手机号是否注册")
    public R sendLoginSmsCode(@RequestBody SendCodeForm request){
        AppUserEntity user = appUserService.lambdaQuery().eq(AppUserEntity::getMobile, request.getMobile()).one();
        if(ObjectUtil.isNull(user)){
            return R.error("该手机号未注册");
        }
        String code=appUserService.sendSmsCode(request);
        String smsOpen = configService.getValue(Constant.SMS_OPEN);
        if("0".equals(smsOpen)){
            //发送阿里云短信
            try {
                smsSenderService.sendLoginCode(request.getMobile(), code);
            } catch (Exception e) {
                String codeKey = "code_" + request.getMobile();
                org.aileme.common.redis.utils.RedisUtils.deleteObject(codeKey);
                e.printStackTrace();
            }
            return R.ok("发送成功,注意查收");
        }
        return R.ok("测试阶段验证码:"+code);
    }

    /**
     * 发送邮箱登录验证码
     *
     */
    @Limit(count = 2)
    @PostMapping("/sendEmailCode")
    @Operation(summary = "发送邮箱登录验证码")
    public R sendEmailCode(@RequestBody SendCodeForm request){

        String code=appUserService.sendEmailCode(request);

        SimpleMailMessage mailMessage = new SimpleMailMessage();//创建一个简单的邮件信息对象
        //设置邮件发送信息的内容
        mailMessage.setSubject("三维矩阵验证码");//标题
        mailMessage.setText("您的验证码为:" + code);//内容
        mailMessage.setFrom(send);//内容为发送方的邮箱地址
        mailMessage.setTo(request.getEmail());//内容为接收方邮箱地址
        mailSender.send(mailMessage);//发送邮件
        return R.ok("发送成功,注意查收");
    }


    /**
     * 手机验证码登录
     */
    @PostMapping("/smsLogin")
    @Operation(summary = "手机验证码登录")
    public R smsLogin(@RequestBody SmsLoginForm form, HttpServletRequest request){

        //用户登录
        Integer userId = appUserService.smsLogin(form,request);

        log.info("=== 登录成功，userId: {}", userId);

        // 使用 Sa-Token 登录并生成 token
        // App端设置 clientId 为 "app"，避免 SecurityConfig 检查失败
        StpUtil.login(userId, new SaLoginParameter()
            .setTimeout(jwtUtils.getAccessTokenValiditySeconds())
            .setExtra("clientid", "app"));

        log.info("=== Sa-Token login 执行完成");
        log.info("=== 当前登录ID: {}", StpUtil.getLoginId());
        log.info("=== 是否已登录: {}", StpUtil.isLogin());

        // 获取生成的 token
        String token = StpUtil.getTokenValue();
        log.info("=== 获取的 token: {}", token);

        if (token == null || token.isEmpty()) {
            log.error("=== token 为空，尝试其他方法获取");
            // 尝试从 TokenInfo 获取
            try {
                token = StpUtil.getTokenInfo().getTokenValue();
                log.info("=== 从 TokenInfo 获取的 token: {}", token);
            } catch (Exception e) {
                log.error("=== 从 TokenInfo 获取 token 失败", e);
            }

            if (token == null || token.isEmpty()) {
                throw new RuntimeException("生成token失败");
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("expire", jwtUtils.getAccessTokenValiditySeconds());

        return R.ok(map);
    }

    /**
     * H5端注册并登录
     *
     */
    @PostMapping("/register")
    @Operation(summary = "H5端注册")
    public R register(@RequestBody SmsLoginForm form, HttpServletRequest request){

        //用户注册并登录
        Integer userId = appUserService.register(form,request);

        // 使用 Sa-Token 登录并生成 token
        StpUtil.login(userId, new SaLoginParameter()
            .setTimeout(jwtUtils.getAccessTokenValiditySeconds())
            .setExtra("clientid", "app"));
        String token = StpUtil.getTokenValue();

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("expire", jwtUtils.getAccessTokenValiditySeconds());

        return R.ok(map);
    }


    @Login
    @GetMapping("/userInfo")
    @Operation(summary = "获取用户信息")
    public Result<AppUserResponse> userInfo(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        AppUserResponse response=appUserService.getUserInfo(user);

        return new Result<AppUserResponse>().ok(response);
    }


    @Login
    @GetMapping("userId")
    @Operation(summary = "获取用户ID")
    public R userInfo(@RequestAttribute("userId") Integer userId){
        return R.ok().put("userId", userId);
    }


    @Login
    @GetMapping("userSetting")
    @Operation(summary = "获取用户隐私设置")
    public Result<AppUserSettingResponse> userSetting(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        AppUserSettingResponse response=userSettingService.userSetting(user.getUid());
        return new Result<AppUserSettingResponse>().ok(response);
    }


    @Login
    @PostMapping("updateUserSetting")
    @NoRepeatSubmit
    @Operation(summary = "更新用户隐私设置")
    public R updateUserSetting(@Parameter(hidden = true) @LoginUser AppUserEntity user,@RequestBody UpdateUserSettingForm param){
        userSettingService.updateUserSetting(user.getUid(),param);
        return R.ok("修改成功");
    }

    @Login
    @PostMapping("/cancelAccount")
    @NoRepeatSubmit
    @Operation(summary = "用户注销账号")
    public R cancelAccount(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                           @RequestBody CancelAccountForm form) {
        ValidatorUtils.validateEntity(form);
        if (!Boolean.TRUE.equals(form.getAcknowledged())) {
            return R.error("请先确认已阅读注销说明");
        }
        if (!"确认注销".equals(StringUtils.trimToEmpty(form.getConfirmText()))) {
            return R.error("确认文案不正确");
        }
        appUserService.cancelAccount(user, form.getReason());
        StpUtil.logout();
        return R.ok("账号已注销");
    }

    @Login
    @PostMapping("/userInfoEdit")
    @Operation(summary = "用户修改个人信息")
    public Result userInfoEdit(@Parameter(hidden = true) @LoginUser AppUserEntity user, @RequestBody AppUserUpdateForm appUserUpdateForm){
        ValidatorUtils.validateEntity(appUserUpdateForm);
        appUserService.updateAppUserInfo(appUserUpdateForm,user);
        boolean autoPersonaTriggered = personaPreGenerationService.triggerProfileRefresh(user.getUid(), "profile_auto_refresh");
        Map<String, Object> result = new HashMap<>();
        result.put("message", "修改成功");
        result.put("autoPersonaTriggered", autoPersonaTriggered);
        result.put("autoPersonaSource", "profile_auto_refresh");
        return new Result().ok(result);
    }


    @Login
    @PostMapping("/addFollow")
    @Operation(summary = "关注用户")
    public Result addFollow(@Parameter(hidden = true) @LoginUser AppUserEntity user, @RequestBody AddFollowForm request){
        appUserService.addFollow(request,user);
        return new Result().ok("关注用户成功");
    }


    @Login
    @PostMapping("/cancelFollow")
    @Operation(summary = "取消关注用户")
    public Result cancelFollow(@Parameter(hidden = true) @LoginUser AppUserEntity user, @RequestBody AddFollowForm request){
        appUserService.cancelFollow(request,user);
        return new Result().ok("取消关注用户成功");
    }



    @Login
    @PostMapping("/userInfoById")
    @Operation(summary = "用户个人主页信息")
    public Result<AppUserInfoResponse> userInfoById(@RequestBody(required = false) AppUserInfoForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        Integer targetUid = request == null ? null : request.getUid();
        if (targetUid == null) {
            if (user == null || user.getUid() == null) {
                throw new LinfengException("登录状态已失效，请重新登录");
            }
            targetUid = user.getUid();
        }
        AppUserInfoResponse response=appUserService.findUserInfoById(targetUid,user);

        return new Result<AppUserInfoResponse>().ok(response);
    }

    @Login
    @GetMapping("/search")
    @Operation(summary = "搜索用户")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "keyword", description = "搜索关键词")
    })
    public Result<AppPageUtils> search(@RequestParam("page") Integer page,
                                       @RequestParam("keyword") String keyword,
                                       @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =appUserService.search(page,keyword,user.getUid());
        return new Result<AppPageUtils>().ok(pages);
    }

    @Login
    @GetMapping("/userFans")
    @Operation(summary = "我的粉丝分页列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "uid", description = "用户id", required = true)
    })
    public Result<AppPageUtils> userFans(@RequestParam("page") Integer page,
                                         @RequestParam("uid") Integer uid,
                                         @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =appUserService.userFans(page,uid,user.getUid());
        return new Result<AppPageUtils>().ok(pages);
    }

    @Login
    @GetMapping("/follow")
    @Operation(summary = "我的关注分页列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<AppPageUtils> follow(@RequestParam("page") Integer page,
                                       @RequestParam("uid") Integer uid,
                                       @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =appUserService.follow(page,uid,user);
        return new Result<AppPageUtils>().ok(pages);
    }


    @GetMapping("/recharge/list")
    @Operation(summary = "用户充值方案")
    public Result<List<RechargeEntity>> recharge(){

        List<RechargeEntity> allRecharge = rechargeService.getAllRecharge();
        return new Result<List<RechargeEntity>>().ok(allRecharge);
    }

    @Login
    @GetMapping("/impressionTagOptions")
    @Operation(summary = "获取印象标签可选项")
    public Result<List<UserTagOptionVo>> impressionTagOptions() {
        List<TagsEntity> tags = tagsService.lambdaQuery()
            .eq(TagsEntity::getStatus, 1)
            .orderByAsc(TagsEntity::getTagCategory)
            .orderByAsc(TagsEntity::getSort)
            .list();
        List<UserTagOptionVo> list = new ArrayList<>();
        for (TagsEntity tag : tags) {
            UserTagOptionVo vo = new UserTagOptionVo();
            vo.setId(tag.getId());
            vo.setName(tag.getTagName());
            list.add(vo);
        }
        return new Result<List<UserTagOptionVo>>().ok(list);
    }

    @Login
    @GetMapping("/tagOptions")
    @Operation(summary = "获取用户自选标签可选项（按分类分组）")
    public Result<List<TagCategoryVo>> tagOptions() {
        List<TagsEntity> tags = tagsService.lambdaQuery()
            .eq(TagsEntity::getStatus, 1)
            .orderByAsc(TagsEntity::getTagCategory)
            .orderByAsc(TagsEntity::getSort)
            .list();

        // 按分类分组
        Map<String, List<UserTagOptionVo>> groupMap = new LinkedHashMap<>();
        for (TagsEntity tag : tags) {
            String category = tag.getTagCategory() != null ? tag.getTagCategory() : "其他";
            groupMap.computeIfAbsent(category, k -> new ArrayList<>());
            UserTagOptionVo vo = new UserTagOptionVo();
            vo.setId(tag.getId());
            vo.setName(tag.getTagName());
            groupMap.get(category).add(vo);
        }

        List<TagCategoryVo> result = new ArrayList<>();
        for (Map.Entry<String, List<UserTagOptionVo>> entry : groupMap.entrySet()) {
            TagCategoryVo categoryVo = new TagCategoryVo();
            categoryVo.setCategory(entry.getKey());
            categoryVo.setTags(entry.getValue());
            result.add(categoryVo);
        }
        return new Result<List<TagCategoryVo>>().ok(result);
    }

    @Login
    @PostMapping("/giveImpression")
    @Operation(summary = "给用户打印象标签")
    public Result<Void> giveImpression(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                       @RequestBody GiveImpressionForm form) {
        if (form.getToUserId() == null || form.getToUserId().equals(user.getUid())) {
            return new Result<Void>().error("不能给自己打标签");
        }
        if (form.getTagIds() == null || form.getTagIds().isEmpty()) {
            // 清空印象
            userImpressionTagService.saveUserImpressions(user.getUid(), form.getToUserId(), Collections.emptyList(), form.getSourceType(), form.getSourceId());
            return new Result<Void>().ok();
        }
        userImpressionTagService.saveUserImpressions(user.getUid(), form.getToUserId(), form.getTagIds(), form.getSourceType(), form.getSourceId());
        return new Result<Void>().ok();
    }

    @Login
    @GetMapping("/impressionSummary")
    @Operation(summary = "获取用户印象标签聚合")
    public Result<List<UserImpressionSummaryVo>> impressionSummary(@RequestParam("userId") Integer userId) {
        Map<Integer, Integer> summary = userImpressionTagService.getImpressionSummary(userId);
        if (summary.isEmpty()) {
            return new Result<List<UserImpressionSummaryVo>>().ok(Collections.emptyList());
        }
        List<Integer> tagIds = new ArrayList<>(summary.keySet());
        List<org.aileme.shejiao.domain.entity.admin.TagsEntity> tags = tagsService.getBatchByIds(tagIds);
        Map<Integer, org.aileme.shejiao.domain.entity.admin.TagsEntity> tagMap = new HashMap<>();
        for (org.aileme.shejiao.domain.entity.admin.TagsEntity tag : tags) {
            tagMap.put(tag.getId(), tag);
        }
        List<UserImpressionSummaryVo> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : summary.entrySet()) {
            org.aileme.shejiao.domain.entity.admin.TagsEntity tag = tagMap.get(entry.getKey());
            if (tag != null) {
                UserImpressionSummaryVo vo = new UserImpressionSummaryVo();
                vo.setId(tag.getId());
                vo.setName(tag.getTagName());
                vo.setCount(entry.getValue());
                list.add(vo);
            }
        }
        list.sort(Comparator.comparingInt(UserImpressionSummaryVo::getCount).reversed());
        return new Result<List<UserImpressionSummaryVo>>().ok(list);
    }

    @Login
    @PostMapping("/systemInfoList")
    @Operation(summary = "系统消息列表")
    public R systemInfoList(@RequestBody ChatListForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        List<MessageEntity> list=appUserService.systemInfoList(request,user);
        return R.ok().put("result", list);
    }

    @GetMapping("/getHotUserList")
    @Operation(summary = "热门博主列表")
    public Result<List<AppHotUserResponse>> getHotUserList(){
        List<AppHotUserResponse> list=appUserService.getHotUserList();
        return new Result<List<AppHotUserResponse>>().ok(list);
    }

    @PostMapping("/getSessionKey")
    @NoRepeatSubmit
    @Operation(summary = "获取微信加密秘钥")
    public R getSessionKey(@RequestBody  WxLoginForm param){
        String appId = wechatRuntimeConfigService.getMiniAppId();
        String appSecret = wechatRuntimeConfigService.getMiniAppSecret();
        JSONObject json = WechatUtil.getSessionKeyOropenid(param.getCode(),appId,appSecret);
        String session_key = json.getString("session_key");
        String openid = json.get("openid").toString();
        return R.ok().put("session_key",session_key).put("openid",openid);
    }

    @PostMapping("/bindWxPhone")
    @Operation(summary = "微信小程序绑定手机号")
    public Result<Object> bindWxPhone(@RequestBody LoginPhoneParam param,HttpServletRequest request){
        Integer userId = appUserService.bindWxPhone(param,request);//如果手机号已被注册过则返回0
        // 使用 Sa-Token 登录并生成 token
        StpUtil.login(userId, new SaLoginParameter()
            .setTimeout(jwtUtils.getAccessTokenValiditySeconds())
            .setExtra("clientid", "app"));
        String token = StpUtil.getTokenValue();
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("expire", jwtUtils.getAccessTokenValiditySeconds());
        return new Result<>().ok(map);
    }
    @GetMapping("/authInfo")
    @Login
    @Operation(summary = "学历认证")
    public Result authInfo(@RequestParam String code,
                           @Parameter(hidden = true) @LoginUser AppUserEntity user){
        user= appUserService.getById(user.getUid());
        user.setEduCode(code);
        Map<String, Object> map = EducationAuthApi.query(code);
        if (map == null || map.isEmpty()) {
            return new Result().error("学历认证失败，请稍后重试");
        }
        String edu= map.get("层次").toString();
        user.setEducation(EduEnum.getBYDesc(edu).getValue());
        String idCard= map.get("证件号码").toString();
        int age= IdCardUtil.countAge(idCard);
        int gendel= IdCardUtil.judgeGender(idCard);
        user.setIdentyCode(idCard);
        user.setAge(age);
        user.setGender(gendel);
        user.setSchool(map.get("学校名称").toString());
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("xueli",map);
        if(StringUtils.isEmpty(user.getInfo())){
            user.setInfo( jsonObject.toJSONString());
        }else{
            JSONObject dbInfo= JSONObject.parseObject(user.getInfo());
            dbInfo.putAll(jsonObject);
            user.setInfo(dbInfo.toJSONString());
        }
        appUserService.updateById(user);
        return new Result();
    }

    /**
     * https://console.cloud.tencent.com/faceid/access
     * @param user
     * @return
     * {
     *     "Response": {
     *         "Result": "0",
     *         "Description": "姓名和身份证号一致",
     *         "Name": "**杰",
     *         "IdCard": "360*************50",
     *         "Sex": "男",
     *         "Nation": "汉",
     *         "Birth": "1986/11/13",
     *         "Address": "江西省九江市庐山河南路57号01室",
     *         "RequestId": "a62f567c-1eea-4ef3-b51a-a9eb9bd84cd9"
     *     }
     * }
     */
    @PostMapping("/realName")
    @Login
    @Operation(summary = "实名认证")
    public Result realName(@RequestParam("name") String name,
                           @RequestParam("idCard") String idCard,
                           @Parameter(hidden = true) @LoginUser AppUserEntity user){
        user = appUserService.getById(user.getUid());

        // 调用实名认证策略
        String txRealJsonStr = IDRealNameAPi.verify(name, idCard);
        if(StringUtils.isNotBlank(txRealJsonStr)){
            JSONObject jsonObject = JSONObject.parseObject(txRealJsonStr);
            String result = jsonObject.getString("Result");
            String description = jsonObject.getString("Description");

            if("PASS".equals(result)) {
                // 认证通过
                user.setIdentyCode(idCard);
                user.setIdentyCertifStatus(1);

                // 保存认证结果到 info 字段
                UserInfo userInfo;
                if(StringUtils.isEmpty(user.getInfo())){
                    userInfo = new UserInfo();
                }else{
                    userInfo = JSONObject.toJavaObject(JSONObject.parseObject(user.getInfo()), UserInfo.class);
                }
                userInfo.setTxRealNameStr(txRealJsonStr);
                user.setInfo(JSONObject.toJSONString(userInfo));

                appUserService.updateById(user);
                return new Result().ok(description);
            } else {
                // 认证失败
                return new Result().error(description);
            }
        }

        return new Result().error("实名认证服务异常，请稍后重试");
    }

    @GetMapping("/isOpen")
    public Result isOpen(){
        String isOpen = configService.getValue(Constant.IS_OPEN);
        boolean enabled = MiniAppTotalSwitchSupport.isEnabled(isOpen, true);
        return new Result<>().ok(enabled ? 0 : 1);
    }

    @Login
    @GetMapping("/getContact")
    public Result<Object> getContact(){
        String contactWeChat = configService.getValue(Constant.CONTACT_WECHAT);
        String contactWeChatQr = configService.getValue(Constant.CONTACT_WECHAT_QR);
        String time = configService.getValue(Constant.CONTACT_TIME);
        String phone = configService.getValue(Constant.CONTACT_PHONE);
        Map<String,String> map=new HashMap<>();
        map.put("wechat",contactWeChat);
        map.put("image",contactWeChatQr);
        map.put("time",time);
        map.put("phone",phone);
        return new Result<>().ok(map);
    }

    // ==================== 访客埋点相关接口 ====================

    @Login
    @PostMapping("/reportVisitDuration")
    @Operation(summary = "上报主页停留时长")
    public Result<Void> reportVisitDuration(@RequestBody Map<String, Object> params,
                                            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer targetUid = (Integer) params.get("targetUid");
        Integer seconds = (Integer) params.get("seconds");
        if (targetUid != null && seconds != null && seconds > 0) {
            profileVisitService.reportDuration(user.getUid(), targetUid, seconds);
        }
        return new Result<Void>().ok();
    }

    @Login
    @GetMapping("/myVisitors")
    @Operation(summary = "我的访客列表")
    public Result<Map<String, Object>> myVisitors(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        // 刷新最新用户信息（检查VIP状态）
        AppUserEntity currentUser = appUserService.getById(user.getUid());
        currentUser = appUserService.vipExpirationCheck(currentUser);

        Map<String, Object> result = new HashMap<>();
        Integer totalCount = profileVisitService.getVisitorCount(user.getUid());
        result.put("total", totalCount);

        if (currentUser.getVip() != null && currentUser.getVip() == 1) {
            // VIP用户：返回完整列表
            List<ProfileVisitorVo> list = profileVisitService.getMyVisitors(user.getUid(), page, size);
            result.put("list", list);
            result.put("isVip", true);
        } else {
            // 非VIP用户：只返回前3条，且不含停留时长和兴趣等级
            List<ProfileVisitorVo> list = profileVisitService.getMyVisitors(user.getUid(), 1, 3);
            for (ProfileVisitorVo vo : list) {
                vo.setTotalDuration(null);
                vo.setInterestLevel(null);
            }
            result.put("list", list);
            result.put("isVip", false);
        }
        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @GetMapping("/interestedInMe")
    @Operation(summary = "对我感兴趣的人（VIP专属）")
    public Result<Object> interestedInMe(@Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AppUserEntity currentUser = appUserService.getById(user.getUid());
        currentUser = appUserService.vipExpirationCheck(currentUser);

        if (currentUser.getVip() == null || currentUser.getVip() != 1) {
            Map<String, Object> result = new HashMap<>();
            result.put("isVip", false);
            result.put("count", profileVisitService.getInterestedVisitors(user.getUid()).size());
            result.put("list", Collections.emptyList());
            return new Result<>().ok(result);
        }

        Map<String, Object> result = new HashMap<>();
        List<ProfileVisitorVo> list = profileVisitService.getInterestedVisitors(user.getUid());
        result.put("isVip", true);
        result.put("count", list.size());
        result.put("list", list);
        return new Result<>().ok(result);
    }

}
