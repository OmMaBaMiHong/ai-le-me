/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * <p>
 * 商业版授权联系技术客服	  wx:  lwwmmzh
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import java.time.Duration;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.springframework.util.StringUtils;
import org.aileme.shejiao.admin.dao.AppUserDao;
import org.aileme.shejiao.admin.dao.PostDao;
import org.aileme.shejiao.admin.dao.UserRechargeDao;
import org.aileme.shejiao.admin.utils.LocalUser;
import org.aileme.shejiao.admin.utils.WechatUtil;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.MessageEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.entity.admin.UserInfo;
import org.aileme.shejiao.domain.entity.app.FollowEntity;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import org.aileme.shejiao.domain.entity.app.NameChangeEntity;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import org.aileme.shejiao.domain.entity.app.UserSettingEntity;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.domain.vo.*;
import org.aileme.shejiao.app.oss.ImageUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@DS("master")
@Service
@Slf4j
public class AppUserServiceImpl extends ServiceImpl<AppUserDao, AppUserEntity> implements AppUserService {

    @Autowired
    private UserTopicService userTopicService;

    @Autowired
    private TopicAdminService topicAdminService;

    @Autowired
    private FollowService followService;

    @Autowired
    private WechatRuntimeConfigService wechatRuntimeConfigService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private MessageService messageService;
    @Autowired
    private AppUserDao userDao;

    @Autowired
    private PostService postService;
    @Autowired
    private PostDao postDao;

    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private UserSignService userSignService;

    @Autowired
    private BillService billService;

    @Autowired
    private SysConfigService configService;

    @Autowired
    private NameChangeService nameChangeService;

    @Autowired
    private LocalUser localUser;

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private ProfileVisitService profileVisitService;

    @Autowired
    private HongniangService hongniangService;

    @Autowired
    private SensitiveService sensitiveService;

    @Autowired
    private UserRechargeDao userRechargeDao;
    @Autowired
    private RecommendLoveService recommendLoveService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<AppUserEntity> queryWrapper = new QueryWrapper<>();
        //模糊查询
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        String type = (String) params.get("type");
        String vipStatus = (String) params.get("vipStatus");
        if (!WechatUtil.isEmpty(key)) {
            if (NumberUtil.isInteger(key)) {
                queryWrapper.lambda().eq(AppUserEntity::getUid, key);
            } else {
                queryWrapper.lambda().like(AppUserEntity::getUsername, key)
                        .or().like(AppUserEntity::getMobile, key)
                        .or().like(AppUserEntity::getLastLoginIp, key);
            }
        }
        if (!WechatUtil.isEmpty(status)) {
            queryWrapper.eq("status", Integer.parseInt(status));
        }
        if (!WechatUtil.isEmpty(vipStatus)) {
            queryWrapper.eq("vip", Integer.parseInt(vipStatus));
        }
        if (!WechatUtil.isEmpty(type)) {
            queryWrapper.eq("type", Integer.parseInt(type));
        }

        queryWrapper.lambda().orderByDesc(AppUserEntity::getUid);
        IPage<AppUserEntity> page = this.page(
                new Query<AppUserEntity>().getPage(params),
                queryWrapper
        );
        //如果不需要对手机号打码就注释掉下面这段
        List<AppUserEntity> records = page.getRecords();
        records.forEach(user -> {
            if (!WechatUtil.isEmpty(user.getMobile())) {
                user.setMobile(WechatUtil.maskMobile(user.getMobile()));
            }
        });
        page.setRecords(records);


        return new PageUtils(page);
    }

    @Override
    public AppPageUtils findTopicUserPage(TopicUserForm form, AppUserEntity user) {

        List<Integer> uids = userTopicService.getUidByTopicId(form.getId());
        if (uids.isEmpty()) {
            return new AppPageUtils(null, 0, 20, form.getPage());
        }
        Page<AppUserEntity> page = new Page<>(form.getPage(), 20);
        QueryWrapper<AppUserEntity> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.lambda().in(AppUserEntity::getUid, uids);
        Page<AppUserEntity> page1 = this.page(page, queryWrapper1);

        AppPageUtils pages = new AppPageUtils(page1);
        List<?> data = pages.getData();
        List<TopicUserResponse> responseList = new ArrayList<>();
        data.forEach(l -> {
            TopicUserResponse topicUserResponse = new TopicUserResponse();
            BeanUtils.copyProperties(l, topicUserResponse);
            Boolean isAdmin = topicAdminService.isAdmin(topicUserResponse.getUid(), form.getId());
            topicUserResponse.setIsAdmin(isAdmin);
            Integer follow = followService.isFollow(user.getUid(), topicUserResponse.getUid());
            topicUserResponse.setHasFollow(follow);
            topicUserResponse.setMobile(WechatUtil.maskMobile(topicUserResponse.getMobile()));
            responseList.add(topicUserResponse);
        });
        pages.setData(responseList);


        return pages;
    }


    @Override
    public AppUserResponse getUserInfo(AppUserEntity users) {
        AppUserResponse response = new AppUserResponse();
        AppUserEntity user = this.getById(users.getUid());
        if (user == null) {
            throw new LinfengException("用户不存在");
        }
        BeanUtils.copyProperties(user, response);
        // figur(String) → figureList(List<String>) 手动转换
        if (org.apache.commons.lang3.StringUtils.isNotBlank(user.getFigur())) {
            response.setFigureList(Arrays.asList(user.getFigur().split(",")));
        }
        // tagStr(String) → tagStr(List<String>) 手动转换
        response.setTagStr(parseTagStr(user.getTagStr()));
        response.setFans(followService.getFans(user.getUid()));
        response.setFollow(followService.getFollowCount(user.getUid()));
        response.setPostNum(postService.getPostNumberByUid(user.getUid()));
        response.setMobile(WechatUtil.maskMobile(user.getMobile()));
        HongniangInfoEntity hongniang = hongniangService.getByUserId(user.getUid());
        if (hongniang != null && hongniang.getId() != null) {
            response.setHongniangId(hongniang.getId());
        }
        // 空值保护：检查VIP状态和过期时间
        if (user.getVip() != null && user.getVip().equals(Constant.VIP_USER)) {
            if (response.getVipExpireTime() != null && response.getVipExpireTime().before(DateUtil.nowDateTime())) {
                user.setVip(Constant.COMMON_USER);
                this.saveOrUpdate(user);
                response.setVip(Constant.COMMON_USER);
            }
        }
        return response;
    }

    @Override
    @DSTransactional
    public Integer wxLogin(WxLoginForm form, HttpServletRequest request) {

        String appId = wechatRuntimeConfigService.getMiniAppId();
        String appSecret = wechatRuntimeConfigService.getMiniAppSecret();
        JSONObject wx = WechatUtil.getOpenId(form.getCode(), appId, appSecret);
        if (ObjectUtil.isNull(wx) || ObjectUtil.isNull(wx.get("openid"))) {
            throw new LinfengException("openid解析失败");
        }
        String openid = wx.get("openid").toString();
        if (!StringUtils.isEmpty(openid)) {
            LambdaQueryWrapper<AppUserEntity> lambdaQueryWrapper = Wrappers.lambdaQuery();
            lambdaQueryWrapper.eq(AppUserEntity::getOpenid, openid);
            AppUserEntity appUserEntity = baseMapper.selectOne(lambdaQueryWrapper);
            if (appUserEntity != null) {
                if (appUserEntity.getStatus() == 1) {
                    throw new LinfengException("该账号已被禁用");
                }
                if (WechatUtil.isEmpty(appUserEntity.getMobile())) {
                    return 0;
                }
                this.vipExpirationCheck(appUserEntity);
                org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + appUserEntity.getUid());
                saveUserLoginIp(appUserEntity, request);
                return appUserEntity.getUid();
            } else {
                return 0;
                /*List<String> list=new ArrayList<>();
                list.add("萌新");
                AppUserEntity appUser = new AppUserEntity();
                appUser.setOpenid(openid);
                appUser.setAvatar(Constant.DEAULT_HEAD);
                appUser.setGender(0);
                appUser.setUsername("LF_"+RandomUtil.randomNumbers(7));
                String tag = JSON.toJSONString(list);
                appUser.setTagStr(tag);
                appUser.setCreateTime(DateUtil.nowDateTime());
                appUser.setUpdateTime(DateUtil.nowDateTime());
                baseMapper.insert(appUser);
                AppUserEntity appUsers = this.lambdaQuery().eq(AppUserEntity::getOpenid, openid).one();
                //新用户默认加入官方圈子
                topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID,appUser);
                saveUserLoginIp(appUsers,request);

                return appUsers.getUid();*/
            }

        } else {
            throw new LinfengException("openid获取失败");
        }
    }


    public void saveUserLoginIp(AppUserEntity userInfo, HttpServletRequest request) {
        String ip = IPUtil.getIp(request);
        userInfo.setLastLoginIp(ip);
        userInfo.setUpdateTime(DateUtil.nowDateTime());

        // 解析干净的省份+城市名写入 locationCity（用于同城推荐）
        String[] provinceCity = IPUtil.getProvinceCityByIp(ip);
        if (provinceCity != null) {
            userInfo.setLocationCity(provinceCity[1]); // 城市名，如"广州市"
            userInfo.setLocationUpdateTime(DateUtil.nowDateTime());
            // 如果用户没有填过城市，顺便补充 province 和 city
            if (userInfo.getCity() == null || userInfo.getCity().isEmpty()
                || userInfo.getCity().contains("电信") || userInfo.getCity().contains("联通") || userInfo.getCity().contains("移动")) {
                userInfo.setProvince(provinceCity[0]);
                userInfo.setCity(provinceCity[1]);
            }
        }

        userDao.updateById(userInfo);
    }

    @Override
    public void updateAppUserInfo(AppUserUpdateForm appUserUpdateForm, AppUserEntity appUser) {
        AppUserEntity user = this.getById(appUser.getUid());

        if (!WechatUtil.isEmpty(appUserUpdateForm.getAvatar())) {
            user.setAvatar(appUserUpdateForm.getAvatar());
            try {
                String mohu = ImageUtil.mohu(appUserUpdateForm.getAvatar());
                if (StringUtils.isEmpty(user.getInfo())) {
                    UserInfo userInfo = new UserInfo();
                    userInfo.setMohuAvatar(mohu);
                    user.setInfo(JSONObject.toJSONString(userInfo));
                } else {
                    UserInfo userInfo = JSONObject.parseObject(user.getInfo(), UserInfo.class);
                    userInfo.setMohuAvatar(mohu);
                    user.setInfo(JSONObject.toJSONString(userInfo));
                }
            } catch (Exception e) {
                // 头像模糊图是增强能力，不应该阻塞资料主链路保存。
                log.warn("生成头像模糊图失败，降级为仅保存原头像: uid={}, avatar={}, reason={}",
                    user.getUid(), appUserUpdateForm.getAvatar(), e.getMessage());
            }
        } else if (appUserUpdateForm.getFigureList() != null && !appUserUpdateForm.getFigureList().isEmpty()) {
            user.setAvatar(appUserUpdateForm.getFigureList().get(0));

        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(appUserUpdateForm.getBirthday())) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date2 = LocalDate.parse(appUserUpdateForm.getBirthday(), fmt);
            user.setAge(date2.until(LocalDate.now()).getYears());
        }
        if (appUserUpdateForm.getFigureList() != null && !appUserUpdateForm.getFigureList().isEmpty()) {
            String str = appUserUpdateForm.getFigureList().stream().collect(Collectors.joining(","));
            user.setFigur(str);
        }
        if (!WechatUtil.isEmpty(appUserUpdateForm.getGender())) {
            user.setGender(appUserUpdateForm.getGender());
        }
        if (!WechatUtil.isEmpty(appUserUpdateForm.getIntro())) {
            //先检测是否违规
            sensitiveService.checkContent(appUserUpdateForm.getIntro());
            user.setIntro(appUserUpdateForm.getIntro());
        }
        if (appUserUpdateForm.getTagStr() != null && !appUserUpdateForm.getTagStr().isEmpty()) {
            user.setTagStr(JSON.toJSONString(appUserUpdateForm.getTagStr()));
        }
        if (!WechatUtil.isEmpty(appUserUpdateForm.getUsername())) {
            //先检测是否违规
            sensitiveService.checkContent(appUserUpdateForm.getUsername());
            if (!appUserUpdateForm.getUsername().equals(user.getUsername())) {
                boolean canChangeName = nameChangeService.canChangeName(user);
                if (canChangeName) {
                    NameChangeEntity name = new NameChangeEntity();
                    name.setCreateTime(DateUtil.nowDateTime());
                    name.setUid(user.getUid());
                    nameChangeService.save(name);
                    user.setUsername(appUserUpdateForm.getUsername());
                } else {
                    throw new LinfengException("本月改名次数已用完");
                }
            }
        }
        if (!WechatUtil.isEmpty(appUserUpdateForm.getEmail())) {
            AppUserEntity one = this.lambdaQuery().eq(AppUserEntity::getEmail, appUserUpdateForm.getEmail())
                    .ne(AppUserEntity::getUid, user.getUid()).one();
            if (ObjectUtil.isNotNull(one)) {
                throw new LinfengException("该邮箱已被绑定");
            }
            user.setEmail(appUserUpdateForm.getEmail());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(appUserUpdateForm.getSelfIntroduction())) {
            sensitiveService.checkContent(appUserUpdateForm.getSelfIntroduction());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(appUserUpdateForm.getLoveDeclaration())) {
            sensitiveService.checkContent(appUserUpdateForm.getLoveDeclaration());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(appUserUpdateForm.getInterest())) {
            sensitiveService.checkContent(appUserUpdateForm.getInterest());
        }
        BeanUtils.copyProperties(appUserUpdateForm, user);

        baseMapper.updateById(user);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + user.getUid());
    }

    @Override
    public void addFollow(AddFollowForm request, AppUserEntity user) {
//		if(request.getId().equals(user.getUid()) ){
//			throw new LinfengException("不能关注自己哦");
//		}
        // 检查是否已经是关注状态(status=0)
        Boolean isFollowed = followService.isFollowOrNot(user.getUid(), request.getId());
        if (isFollowed) {
            throw new LinfengException("请勿重复关注");
        }

        // 检查是否存在任何记录（包括不喜欢状态）
        FollowEntity existFollow = followService.lambdaQuery()
                .eq(FollowEntity::getUid, user.getUid())
                .eq(FollowEntity::getFollowUid, request.getId())
                .one();

        if (existFollow != null) {
            // 存在记录，更新为关注状态
            existFollow.setStatus(0);
            existFollow.setCreateTime(DateUtil.nowDateTime());
            followService.updateById(existFollow);
        } else {
            // 不存在记录，创建新记录
            FollowEntity followEntity = new FollowEntity();
            followEntity.setCreateTime(DateUtil.nowDateTime());
            followEntity.setFollowUid(request.getId());
            followEntity.setUid(user.getUid());
            followEntity.setStatus(0); // 0表示关注
            followService.save(followEntity);
        }

        int status = followService.isFollow(user.getUid(), request.getId());
        if (status == FollowEnums.MUTUAL_FOLLOW.getCode()) {
            friendService.doFirendsEach(user, request.getId());
        }
        String content = StrUtil.format(Constant.CONTENT_WATCH, user.getUsername());
        messageService.sendMessage(user.getUid(), request.getId(), 0, Constant.WATCH, content, Constant.TITLE_WATCH);
    }

    @Override
    public void cancelFollow(AddFollowForm request, AppUserEntity user) {

        // 通过FollowService删除关注关系
        followService.lambdaUpdate()
                .eq(FollowEntity::getUid, user.getUid())
                .eq(FollowEntity::getFollowUid, request.getId())
                .remove();
        if (friendService.checkIsFriend(user.getUid(), request.getId())) {
            friendService.removeFriends(user.getUid(), request.getId());
        }
    }

    @Override
    public AppUserInfoResponse findUserInfoById(Integer uid, AppUserEntity user) {
        if (uid == null || uid <= 0) {
            throw new LinfengException("用户不存在");
        }
        if (user == null || user.getUid() == null) {
            throw new LinfengException("登录状态已失效，请重新登录");
        }
        AppUserEntity userEntity = this.getById(uid);
        if (userEntity == null) {
            throw new LinfengException("用户不存在");
        }
        AppUserInfoResponse response = new AppUserInfoResponse();
        BeanUtils.copyProperties(userEntity, response);
        response.setFigureList(parseFigureList(userEntity.getFigur()));
        // tagStr(String) → tagStr(List<String>) 手动转换
        response.setTagStr(parseTagStr(userEntity.getTagStr()));
        try {
            AppPageUtils appPageUtils = topicService.myCreateTopic(1, userEntity);
            List<TopicListResponse> data = appPageUtils == null ? Collections.emptyList() : (List<TopicListResponse>) appPageUtils.getData();
            response.setCreateTopicList(data == null ? Collections.emptyList() : data);
        } catch (Exception e) {
            log.error("加载用户创建圈子失败, targetUid={}", uid, e);
            response.setCreateTopicList(Collections.emptyList());
        }
        response.setFans(followService.getFans(userEntity.getUid()));
        response.setFollow(followService.getFollowCount(userEntity.getUid()));
        response.setPostNum(postService.getPostNumberByUid(userEntity.getUid()));
        response.setIsFollow(followService.isFollowOrNot(user.getUid(), uid));
        response.setMobile(WechatUtil.maskMobile(userEntity.getMobile()));
        response.setIsFriend(friendService.checkIsFriend(user.getUid(), uid));
        // 查询与当前登录用户的会话ID（如果是好友）
        if (!uid.equals(user.getUid())) {
            List<FriendEntity> friendList = friendService.lambdaQuery()
                    .eq(FriendEntity::getMyId, user.getUid())
                    .eq(FriendEntity::getFriendId, uid)
                    .orderByDesc(FriendEntity::getUpdateTime)
                    .orderByDesc(FriendEntity::getId)
                    .list();
            if (friendList != null && friendList.size() > 1) {
                log.warn("检测到重复好友关系数据，myId={}, friendId={}, size={}", user.getUid(), uid, friendList.size());
            }
            FriendEntity friendEntity = (friendList == null || friendList.isEmpty()) ? null : friendList.get(0);
            if (friendEntity != null && friendEntity.getSessionId() != null) {
                response.setSessionId(String.valueOf(friendEntity.getSessionId()));
            }
        }
        try {
            AppPageUtils pageUtils = postService.myPost(1, userEntity);
            List<PostEntity> posts = pageUtils == null ? Collections.emptyList() : (List<PostEntity>) pageUtils.getData();
            response.setPostEntities(posts == null ? Collections.emptyList() : posts);
        } catch (Exception e) {
            log.error("加载用户帖子失败, targetUid={}", uid, e);
            response.setPostEntities(Collections.emptyList());
        }
        //用户隐私设置
        UserSettingEntity userSetting = userSettingService.lambdaQuery().eq(UserSettingEntity::getUid, uid).one();
        if (ObjectUtil.isNull(userSetting)) {
            response.setIsFan(false);
            response.setIsWatch(false);
            response.setIsPost(false);
        } else {
            if (uid.equals(user.getUid())) {
                response.setIsFan(false);
                response.setIsWatch(false);
                response.setIsPost(false);
            } else {
                response.setIsFan(userSetting.getIsFollow() == 1);
                response.setIsPost(userSetting.getIsPost() == 1);
                response.setIsWatch(userSetting.getIsWatch() == 1);
            }
        }

        // 访客埋点：记录访问（不记录自己访问自己）
        if (!uid.equals(user.getUid())) {
            try {
                profileVisitService.recordVisit(user.getUid(), uid);
            } catch (Exception e) {
                log.warn("访客埋点记录失败: {}", e.getMessage());
            }
        }
        // 访客统计（自己主页可见）
        try {
            response.setVisitorCount(profileVisitService.getVisitorCount(uid));
            List<ProfileVisitorVo> recentVisitors = profileVisitService.getRecentVisitors(uid, 3);
            response.setRecentVisitors(recentVisitors == null ? Collections.emptyList() : recentVisitors);
        } catch (Exception e) {
            log.error("加载访客数据失败, targetUid={}", uid, e);
            response.setVisitorCount(0);
            response.setRecentVisitors(Collections.emptyList());
        }

        // 契合度计算（查看他人主页时）
        if (!uid.equals(user.getUid())) {
            try {
                calculateMatchScore(user, userEntity, response);
            } catch (Exception e) {
                log.error("计算契合度失败, loginUid={}, targetUid={}", user.getUid(), uid, e);
            }
        }

        return response;
    }

    private List<String> parseFigureList(String figur) {
        if (org.apache.commons.lang3.StringUtils.isBlank(figur)) {
            return Collections.emptyList();
        }
        String raw = figur.trim();
        try {
            if (raw.startsWith("[")) {
                List<String> parsed = JSON.parseArray(raw, String.class);
                if (parsed != null) {
                    return parsed.stream()
                            .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                            .map(String::trim)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("解析用户形象照失败, figur={}", org.apache.commons.lang3.StringUtils.abbreviate(raw, 160), e);
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public AppPageUtils search(Integer currPage, String keyword, Integer uid) {

        Page<AppUserEntity> p = new Page<>(currPage, 10);
        QueryWrapper<AppUserEntity> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.like("username", keyword);
        queryWrapper1.orderByDesc("uid");
        Page<AppUserEntity> page1 = this.page(p, queryWrapper1);

        AppPageUtils pages = new AppPageUtils(page1);
        List<?> data = pages.getData();
        List<TopicUserResponse> responseList = new ArrayList<>();
        data.forEach(l -> {
            TopicUserResponse topicUserResponse = new TopicUserResponse();
            BeanUtils.copyProperties(l, topicUserResponse);
            Integer follow = followService.isFollow(uid, topicUserResponse.getUid());
            topicUserResponse.setHasFollow(follow);
            topicUserResponse.setMobile(WechatUtil.maskMobile(topicUserResponse.getMobile()));
            responseList.add(topicUserResponse);
        });
        pages.setData(responseList);


        return pages;
    }


    @Override
    public AppPageUtils userFans(Integer currPage, Integer target, Integer uid) {
        if (target != 0) {
            uid = target;
        }
        List<Integer> uidList = followService.getFansList(uid);
        if (uidList.isEmpty()) {
            return new AppPageUtils(null, 0, 10, currPage);
        }
        Page<AppUserEntity> page = new Page<>(currPage, 10);
        QueryWrapper<AppUserEntity> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.lambda().in(AppUserEntity::getUid, uidList);
        Page<AppUserEntity> page1 = this.page(page, queryWrapper1);

        AppPageUtils pages = new AppPageUtils(page1);
        List<?> data = pages.getData();
        List<TopicUserResponse> responseList = new ArrayList<>();
        Integer finalUid = uid;
        data.forEach(l -> {
            TopicUserResponse topicUserResponse = new TopicUserResponse();
            BeanUtils.copyProperties(l, topicUserResponse);
            Integer follow = followService.isFollow(finalUid, topicUserResponse.getUid());
            topicUserResponse.setHasFollow(follow);
            topicUserResponse.setMobile(WechatUtil.maskMobile(topicUserResponse.getMobile()));
            responseList.add(topicUserResponse);
        });
        pages.setData(responseList);
        return pages;
    }

    @Override
    public AppPageUtils follow(Integer currPage, Integer uid, AppUserEntity user) {
        if (uid == 0) {
            uid = user.getUid();
        }
        List<FollowEntity> list = followService.lambdaQuery()
                .eq(FollowEntity::getUid, uid)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .list();
        List<Integer> followUidList = list.stream().map(FollowEntity::getFollowUid).collect(Collectors.toList());
        if (followUidList.isEmpty()) {
            return new AppPageUtils(null, 0, 10, currPage);
        }
        Page<AppUserEntity> page = new Page<>(currPage, 10);
        QueryWrapper<AppUserEntity> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.lambda().in(AppUserEntity::getUid, followUidList);
        Page<AppUserEntity> page1 = this.page(page, queryWrapper1);

        AppPageUtils pages = new AppPageUtils(page1);
        List<?> data = pages.getData();
        List<TopicUserResponse> responseList = new ArrayList<>();
        data.forEach(l -> {
            TopicUserResponse topicUserResponse = new TopicUserResponse();
            BeanUtils.copyProperties(l, topicUserResponse);
            Integer follow = followService.isFollow(user.getUid(), topicUserResponse.getUid());
            topicUserResponse.setHasFollow(follow);
            topicUserResponse.setMobile(WechatUtil.maskMobile(topicUserResponse.getMobile()));
            responseList.add(topicUserResponse);
        });
        pages.setData(responseList);
        return pages;
    }

    @Override
    public void ban(Integer id) {
        Integer status = this.lambdaQuery().eq(AppUserEntity::getUid, id).one().getStatus();
        if (status == 1) {
            throw new LinfengException("该账号已被禁用");
        }
        UpdateWrapper<AppUserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", 1);
        updateWrapper.eq("uid", id);
        update(updateWrapper);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + id);
    }

    @Override
    public void openBan(Integer id) {
        Integer status = this.lambdaQuery().eq(AppUserEntity::getUid, id).one().getStatus();
        if (status == 0) {
            throw new LinfengException("该账号已解除禁用");
        }
        UpdateWrapper<AppUserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", 0);
        updateWrapper.eq("uid", id);
        update(updateWrapper);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + id);
    }

    @Override
    public String sendSmsCode(SendCodeForm request) {
        System.out.println("=== sendSmsCode 方法被调用 - mobile: " + request.getMobile());
        String code = RandomUtil.randomNumbers(Constant.SMS_SIZE);
        String codeKey = "code_" + request.getMobile();
        System.out.println("=== 生成验证码 - code: " + code + ", codeKey: " + codeKey);
        String s = readVerificationCode(codeKey);
        System.out.println("=== 查询已存在验证码 - existing: " + s);
        if (ObjectUtil.isNotNull(s)) {
            System.out.println("=== 返回已存在的验证码: " + s);
            return s;
        }
        System.out.println("=== 准备存储新验证码到Redis");
        org.aileme.common.redis.utils.RedisUtils.setCacheObject(codeKey, String.valueOf(code), Duration.ofSeconds(300));
        System.out.println("=== 验证码存储完成，返回: " + code);
        return code;
    }

    @Override
    public String sendEmailCode(SendCodeForm request) {
        String code = RandomUtil.randomNumbers(Constant.SMS_SIZE);
        String codeKey = "code_" + request.getEmail();
        String s = readVerificationCode(codeKey);
        if (ObjectUtil.isNotNull(s)) {
            return s;
        }
        org.aileme.common.redis.utils.RedisUtils.setCacheObject(codeKey, String.valueOf(code), Duration.ofSeconds(300));
        return code;
    }

    /**
     * 手机号登录和邮箱登录
     * @param form
     * @param request
     * @return
     */
    @Override
    public Integer smsLogin(SmsLoginForm form, HttpServletRequest request) {
        System.out.println("=== smsLogin 方法被调用 - mobile: " + form.getMobile() + ", code: " + form.getCode());
        if (!WechatUtil.isEmpty(form.getMobile())) {
            AppUserEntity appUserEntity = this.lambdaQuery().eq(AppUserEntity::getMobile, form.getMobile()).one();
            String codeKey = "code_" + form.getMobile();
            System.out.println("=== 准备从Redis获取验证码 - codeKey: " + codeKey);
            String s = readVerificationCode(codeKey);
            System.out.println("=== 从Redis获取到的验证码 - value: " + s + ", 是否为空: " + WechatUtil.isEmpty(s));
            if (WechatUtil.isEmpty(s)) {
                throw new LinfengException("请先发送验证码");
            }
            System.out.println("=== 验证码对比 - Redis中的: [" + s + "], 用户输入的: [" + form.getCode() + "], 相等: " + s.equals(form.getCode()));
            if (!s.equals(form.getCode())) {
                throw new LinfengException("验证码错误！");
            }
            if (appUserEntity != null) {
                if (appUserEntity.getStatus() != null && appUserEntity.getStatus() == 1) {
                    throw new LinfengException("该账号已被禁用");
                }
                this.vipExpirationCheck(appUserEntity);
                saveUserLoginIp(appUserEntity, request);
                return appUserEntity.getUid();
            } else {
                List<String> list = new ArrayList<>();
                list.add("萌新");
                AppUserEntity appUser = new AppUserEntity();
                appUser.setMobile(form.getMobile());
                appUser.setAvatar(Constant.DEAULT_HEAD);
                appUser.setGender(0);
                appUser.setUsername("MM_" + RandomUtil.randomNumbers(7));
                appUser.setTagStr(list.toString());
                appUser.setCreateTime(DateUtil.nowDateTime());
                appUser.setUpdateTime(DateUtil.nowDateTime());
                baseMapper.insert(appUser);
                AppUserEntity appUsers = this.lambdaQuery().eq(AppUserEntity::getMobile, form.getMobile()).one();
                //新用户默认加入官方圈子
                topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID, appUsers);
                saveUserLoginIp(appUsers, request);

                return appUsers.getUid();
            }
        } else if (!WechatUtil.isEmpty(form.getEmail())) {
            AppUserEntity appUserEntity = this.lambdaQuery().eq(AppUserEntity::getEmail, form.getEmail()).one();
            String codeKey = "code_" + form.getEmail();
            String s = readVerificationCode(codeKey);
            if (!s.equals(form.getCode())) {
                throw new LinfengException("验证码错误！");
            }
            if (appUserEntity != null) {
                if (appUserEntity.getStatus() != null && appUserEntity.getStatus() == 1) {
                    throw new LinfengException("该账号已被禁用");
                }
                saveUserLoginIp(appUserEntity, request);
                return appUserEntity.getUid();
            } else {
                throw new LinfengException("邮箱不存在请先注册");

            }
        } else {
            throw new LinfengException("登录参数异常");
        }


    }

    @Override
    public Integer register(SmsLoginForm form, HttpServletRequest request) {
        if (WechatUtil.isEmpty(form.getMobile())) {
            throw new LinfengException("注册账号必须绑定手机号");
        }
        AppUserEntity appUserEntity = this.lambdaQuery().eq(AppUserEntity::getMobile, form.getMobile()).one();
        String codeKey = "code_" + form.getMobile();
        String s = readVerificationCode(codeKey);
        if (WechatUtil.isEmpty(s)) {
            throw new LinfengException("请先发送验证码");
        }
        if (!s.equals(form.getCode())) {
            throw new LinfengException("验证码错误！");
        }
        if (appUserEntity != null) {
            throw new LinfengException("该手机号已注册");
        } else {
            AppUserEntity appUser = new AppUserEntity();
            if (!WechatUtil.isEmpty(form.getEmail())) {
                AppUserEntity userEntity = this.lambdaQuery().eq(AppUserEntity::getEmail, form.getEmail()).one();
                if (userEntity != null) {
                    throw new LinfengException("该邮箱已注册");
                }
                appUser.setEmail(form.getEmail());
            }
            List<String> list = new ArrayList<>();
            list.add("萌新");
            appUser.setMobile(form.getMobile());
//            appUser.setAvatar(Constant.DEAULT_HEAD);
            appUser.setGender(0);
            appUser.setUsername(generateNaturalDefaultUsername(0, null, form.getMobile()));
            appUser.setTagStr(list.toString());
            appUser.setCreateTime(DateUtil.nowDateTime());
            appUser.setUpdateTime(DateUtil.nowDateTime());
            baseMapper.insert(appUser);
            AppUserEntity appUsers = this.lambdaQuery().eq(AppUserEntity::getMobile, form.getMobile()).one();
            //新用户默认加入官方圈子
            topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID, appUsers);
            saveUserLoginIp(appUsers, request);
            //推荐设置
            RecommendLoveEntity recommendLoveEntity = new RecommendLoveEntity();
            recommendLoveEntity.setUid(appUsers.getUid());
            recommendLoveEntity.setRecommendUid(0);
            recommendLoveService.save(recommendLoveEntity);

            return appUsers.getUid();
        }


    }

    private String readVerificationCode(String codeKey) {
        Object cached = org.aileme.common.redis.utils.RedisUtils.getCacheObject(codeKey);
        if (cached == null) {
            return null;
        }
        return String.valueOf(cached);
    }


    @Override
    public Integer getTotalNum() {
        return Math.toIntExact(this.lambdaQuery().select(AppUserEntity::getUid).count());
    }

    private String generateNaturalDefaultUsername(Integer gender, String city, String seed) {
        for (int i = 0; i < 8; i++) {
            String candidate = NaturalUsernameGenerator.generate(gender, city, seed + i);
            if (this.lambdaQuery().eq(AppUserEntity::getUsername, candidate).count() == 0) {
                return candidate;
            }
        }
        return NaturalUsernameGenerator.generate(gender, city, seed) + RandomUtil.randomNumbers(2);
    }

    @Override
    public Integer getRegisterNumByDate(String date) {
        QueryWrapper<AppUserEntity> wrapper = Wrappers.query();
        wrapper.select("uid");
        wrapper.apply("date_format(create_time, '%Y-%m-%d') = {0}", date);
        return Math.toIntExact(userDao.selectCount(wrapper));
    }

    /**
     * 首页面板数据
     * @return
     */
    @Override
    public HomeRateResponse indexDate() {
        String today = cn.hutool.core.date.DateUtil.date().toString("yyyy-MM-dd");
        String yesterday = cn.hutool.core.date.DateUtil.yesterday().toString("yyyy-MM-dd");
        Long count = postService.count(Wrappers.<PostEntity>lambdaQuery().eq(PostEntity::getStatus, Constant.POST_REVIEWED));
        Long postCount = postService.count(Wrappers.<PostEntity>lambdaQuery().select(PostEntity::getId));
        double rechargeMoney = userRechargeService.rechargeMoney();
        double rechargeMoneyByMonth = userRechargeService.rechargeMoneyByMonth();
        Integer userCount = userSignService.getSignUserCount();
        HomeRateResponse response = new HomeRateResponse();
        response.setTotalPostOfReview(count);
        response.setTotalPost(postCount);
        response.setNewUserNum(this.getRegisterNumByDate(today));
        response.setYesterdayNewUserNum(this.getRegisterNumByDate(yesterday));
        response.setTotalUser(this.getTotalNum());
        response.setRechargeMoney(rechargeMoney);
        response.setRechargeMoneyByMonth(rechargeMoneyByMonth);
        response.setUserSignCount(userCount);
        return response;
    }

    /**
     * 本月新增用户
     *
     * @return map
     */
    @Override
    public Map<String, Object> chartCount() {
        Map<String, Object> map = new LinkedHashMap<>();
//        Date month = cn.hutool.core.date.DateUtil.beginOfMonth(new Date());
        Date month = cn.hutool.core.date.DateUtil.lastMonth();
        map.put("chart", userDao.chartList(month));
        return map;
    }

    @Override
    public Map<String, Object> chartPost() {
        Map<String, Object> map = new LinkedHashMap<>();
//        Date month = cn.hutool.core.date.DateUtil.beginOfMonth(new Date());
        Date month = cn.hutool.core.date.DateUtil.lastMonth();
        map.put("postChart", postDao.chartList(month));

        return map;
    }

    /**
     * 本月交易额统计
     * @return
     */
    @Override
    public Map<String, Object> chartMoney() {
        Map<String, Object> map = new LinkedHashMap<>();
        Date month = cn.hutool.core.date.DateUtil.beginOfMonth(new Date());
//        Date month = cn.hutool.core.date.DateUtil.lastMonth();
        map.put("chartMoney", userRechargeDao.chartList(month));

        return map;
    }

    @Override
    public void punishUser(AdminUserPunishForm param) {
        AppUserEntity user = this.getById(param.getUid());
        if (user == null) {
            throw new LinfengException("用户不存在");
        }
        if (param.getResetAvatar() == 1) {
            user.setAvatar(Constant.DEAULT_HEAD);
        }
        if (param.getResetIntro() == 1) {
            user.setIntro(Constant.DEAULT_INTRO);
        }
        if (param.getResetUsername() == 1) {
            user.setUsername("MM_" + RandomUtil.randomNumbers(7));
        }
        if (param.getResetPost() == 1) {
            List<PostEntity> list = postService.lambdaQuery()
                    .eq(PostEntity::getUid, param.getUid())
                    .ne(PostEntity::getStatus, Constant.POST_BANNER).list();
            list.forEach(item -> {
                item.setStatus(Constant.POST_BANNER);
            });
            boolean down = postService.updateBatchById(list);
            if (!down) {
                throw new LinfengException("帖子下架失败");
            }
        } else if (param.getResetPost() == 2) {
            List<PostEntity> list = postDao.selectList(Wrappers.<PostEntity>lambdaQuery()
                    .eq(PostEntity::getUid, param.getUid()));
            if (list.size() > 0) {
                List<Integer> postIdList = list.stream().map(PostEntity::getId).collect(Collectors.toList());
                boolean remove = this.removeByIds(postIdList);
                if (!remove) {
                    throw new LinfengException("帖子删除失败");
                }
            }
        }
        this.updateById(user);
    }

    @Override
    public AppUserEntity saveMpWxUser(WxMpUser wxMpUser) {
        if (ObjectUtil.isNull(wxMpUser) || StrUtil.isBlank(wxMpUser.getOpenId())) {
            throw new LinfengException("公众号用户信息不能为空");
        }

        String mpOpenId = wxMpUser.getOpenId();
        String unionId = StrUtil.trimToEmpty(wxMpUser.getUnionId());
        LambdaQueryWrapper<AppUserEntity> queryWrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(unionId)) {
            queryWrapper.and(wrapper -> wrapper.eq(AppUserEntity::getMpOpenid, mpOpenId)
                    .or()
                    .eq(AppUserEntity::getUnionid, unionId));
        } else {
            queryWrapper.eq(AppUserEntity::getMpOpenid, mpOpenId);
        }
        queryWrapper.last("limit 2");

        List<AppUserEntity> candidates = baseMapper.selectList(queryWrapper);
        if (candidates.size() > 1) {
            log.warn("检测到重复公众号用户记录，mpOpenId={}, unionId={}, uidList={}",
                    mpOpenId, unionId, candidates.stream().map(AppUserEntity::getUid).collect(Collectors.toList()));
        }

        boolean isNewUser = candidates.isEmpty();
        AppUserEntity appUser = isNewUser ? new AppUserEntity() : candidates.get(0);
        if (isNewUser) {
            List<String> defaultTags = new ArrayList<>();
            defaultTags.add(Constant.DEAULT_TAG);
            appUser.setMpOpenid(mpOpenId);
            appUser.setUnionid(StrUtil.emptyToNull(unionId));
            appUser.setAvatar(StrUtil.blankToDefault(wxMpUser.getHeadImgUrl(), Constant.DEAULT_HEAD));
            appUser.setGender(resolveWxGender());
            appUser.setUsername(buildMpUsername(wxMpUser));
            appUser.setTagStr(JSON.toJSONString(defaultTags));
            appUser.setCreateTime(DateUtil.nowDateTime());
        } else {
            appUser.setMpOpenid(mpOpenId);
            if (StrUtil.isNotBlank(unionId)) {
                appUser.setUnionid(unionId);
            }
            if (StrUtil.isNotBlank(wxMpUser.getHeadImgUrl())) {
                appUser.setAvatar(wxMpUser.getHeadImgUrl());
            } else if (StrUtil.isBlank(appUser.getAvatar())) {
                appUser.setAvatar(Constant.DEAULT_HEAD);
            }
            if (StrUtil.isNotBlank(wxMpUser.getNickname())) {
                appUser.setUsername(wxMpUser.getNickname());
            } else if (StrUtil.isBlank(appUser.getUsername())) {
                appUser.setUsername(buildMpUsername(wxMpUser));
            }
            if (appUser.getGender() == null || appUser.getGender() == 0) {
                appUser.setGender(resolveWxGender());
            }
            if (StrUtil.isBlank(appUser.getTagStr())) {
                appUser.setTagStr(JSON.toJSONString(Collections.singletonList(Constant.DEAULT_TAG)));
            }
        }
        appUser.setUpdateTime(DateUtil.nowDateTime());

        if (isNewUser) {
            baseMapper.insert(appUser);
            topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID, appUser);
            if (recommendLoveService.lambdaQuery().eq(RecommendLoveEntity::getUid, appUser.getUid()).count() == 0) {
                RecommendLoveEntity recommendLoveEntity = new RecommendLoveEntity();
                recommendLoveEntity.setUid(appUser.getUid());
                recommendLoveEntity.setRecommendUid(0);
                recommendLoveService.save(recommendLoveEntity);
            }
        } else {
            baseMapper.updateById(appUser);
            org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + appUser.getUid());
        }
        return appUser;
    }

    private Integer resolveWxGender() {
        return 0;
    }

    private String buildMpUsername(WxMpUser wxMpUser) {
        if (wxMpUser != null && StrUtil.isNotBlank(wxMpUser.getNickname())) {
            return wxMpUser.getNickname();
        }
        return "WX_" + RandomUtil.randomNumbers(7);
    }


    /**
     * 更新用户余额
     * @param uid
     * @param price
     * @param givePrice
     */
    @Override
    public BigDecimal updateMoney(Integer uid, BigDecimal price, BigDecimal givePrice) {
        AppUserEntity user = this.getById(uid);
        BigDecimal money = user.getMoney().add(price).add(givePrice);
        boolean update = this.lambdaUpdate()
                .eq(AppUserEntity::getUid, uid)
                .set(AppUserEntity::getMoney, money)
                .update();
        if (!update) {
            throw new LinfengException("更新用户余额失败");
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + user.getUid());
        return money;

    }

    /**
     * 管理端更新用户
     * @param user
     */
    @Override
    @DSTransactional
    public void updateUser(AdminUserInfoForm user) {
        AppUserEntity appUser = this.getById(user.getUid());
        appUser.setType(user.getType());
        appUser.setStatus(user.getStatus());
        if (user.getChangeMoney() == 0) {
            if (user.getChangeValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new LinfengException("金额必须大于0");
            }
            if (user.getUpOrDown() == 0) {
                //增加余额
                appUser.setMoney(appUser.getMoney().add(user.getChangeValue()));
                billService.income(appUser.getUid(), BillDetailEnum.TYPE_6.getDesc(),
                        BillDetailEnum.CATEGORY_1.getValue(), BillDetailEnum.TYPE_6.getValue(),
                        user.getChangeValue().doubleValue(), appUser.getMoney().doubleValue(),
                        "后台系统增加余额", "", null);
            } else if (user.getUpOrDown() == 1) {
                //减少余额
                appUser.setMoney(appUser.getMoney().subtract(user.getChangeValue()));
                billService.expend(appUser.getUid(), BillDetailEnum.TYPE_7.getDesc(),
                        BillDetailEnum.CATEGORY_1.getValue(), BillDetailEnum.TYPE_7.getValue(),
                        user.getChangeValue().doubleValue(), appUser.getMoney().doubleValue(),
                        "后台系统减少余额", "", null);
            }
        }
        boolean b = this.updateById(appUser);
        if (!b) {
            throw new LinfengException("更新失败");
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + appUser.getUid());

    }

    @Override
    public List<AppUserEntity> getBatchUser(List<Integer> uid) {
        return userDao.getBatchUser(uid);
    }

    @Override
    public List<MessageEntity> systemInfoList(ChatListForm request, AppUserEntity user) {
        Integer toUid = request.getUid();
        Integer fromUid = user.getUid();
        List<MessageEntity> toList = (List<MessageEntity>) messageService.getSystemList(toUid, fromUid, request.getPage()).getData();
        return toList.stream().sorted(Comparator.comparing(MessageEntity::getMId)).collect(Collectors.toList());
    }

    /**
     * 检查用户会员是否过期
     * @param user
     */
    @Override
    public AppUserEntity vipExpirationCheck(AppUserEntity user) {
        if (user.getVip().equals(Constant.VIP_USER)) {
            boolean isBefore = user.getVipExpireTime().before(DateUtil.nowDateTime());
            if (isBefore) {
                user.setVip(Constant.COMMON_USER);
                this.saveOrUpdate(user);
            }
        }
        return user;
    }

    /**
     * 热门博主
     * 加缓存
     * @return
     */
    @Override
    public List<AppHotUserResponse> getHotUserList() {
        String res = org.aileme.common.redis.utils.RedisUtils.getCacheObject(ConfigConstant.HOT_USER_KEY);
        AppUserEntity user = localUser.getUser();
        boolean isNull = ObjectUtil.isNull(user);
        if (WechatUtil.isEmpty(res)) {
            //查询近一个月粉丝增长最快的用户
            DateTime dateTime = cn.hutool.core.date.DateUtil.lastMonth();
            List<HotUserResponse> list = followService.getHotUserList(dateTime);
            if (list.isEmpty()) {
                return new ArrayList<>();
            }
            List<AppHotUserResponse> result = new ArrayList<>();
            list.forEach(item -> {
                AppHotUserResponse response = new AppHotUserResponse();
                BeanUtils.copyProperties(this.getById(item.getUid()), response);
                response.setFans(followService.getFans(item.getUid()));
                response.setPostNum(postService.getPostNumberByUid(item.getUid()));
                if (isNull) {
                    response.setIsFollow(false);
                } else {
                    response.setIsFollow(followService.isFollowOrNot(user.getUid(), item.getUid()));
                }
                result.add(response);
            });
            org.aileme.common.redis.utils.RedisUtils.setCacheObject(ConfigConstant.HOT_USER_KEY, JSON.toJSON(result).toString(), Duration.ofSeconds(60 * 30));
            return result;
        }
        List<AppHotUserResponse> list = JSONObject.parseArray(res, AppHotUserResponse.class);
        //判断用户是否关注
        Map<Integer, Integer> followCollectMap = new HashMap<>();
        if (!isNull) {
            List<Integer> uidList = list.stream().map(AppHotUserResponse::getUid).collect(Collectors.toList());
            List<FollowBatchResponse> followBatch = followService.findFollowBatch(uidList, user.getUid());
            followCollectMap = followBatch.stream().collect(Collectors.toMap(FollowBatchResponse::getFollowUid, FollowBatchResponse::getId));
        }
        Map<Integer, Integer> finalFollowCollectMap = followCollectMap;
        list.forEach(item -> {
            if (isNull) {
                item.setIsFollow(false);
            } else {
                if (ObjectUtil.isNotNull(finalFollowCollectMap.get(item.getUid()))) {
                    item.setIsFollow(true);
                } else {
                    item.setIsFollow(false);
                }
            }
        });
        return list;

    }

    /**
     * 微信小程序绑定手机号
     * @param param
     * @param request
     * @return
     */
    @Override
    public Integer bindWxPhone(LoginPhoneParam param, HttpServletRequest request) {
        Map<String, Object> map = WechatUtil.getPhoneNumber(param);
        if (WechatUtil.isEmpty(map)) {
            throw new LinfengException("微信授权失败");
        }
        String phone = "";
        Object phoneNumber = map.get("param");
        String jsonString = JSONObject.toJSONString(phoneNumber);
        JSONObject obj = JSONObject.parseObject(jsonString);
        if (!WechatUtil.strIsEmpty(jsonString)) {
            phone = obj.get("phoneNumber").toString();
        }
        AppUserEntity target = this.lambdaQuery().eq(AppUserEntity::getMobile, phone).one();
        //如果用户手机号之前未注册过，那么正常绑定即可
        if (target == null) {
            //注册本用户
            List<String> list = new ArrayList<>();
            list.add("萌新");
            AppUserEntity appUser = new AppUserEntity();
            appUser.setMobile(phone);
            appUser.setOpenid(param.getWechatOpenId());
            appUser.setAvatar(Constant.DEAULT_HEAD);
            appUser.setGender(0);
            appUser.setUsername("MM_" + RandomUtil.randomNumbers(7));
            appUser.setTagStr(list.toString());
            appUser.setCreateTime(DateUtil.nowDateTime());
            appUser.setUpdateTime(DateUtil.nowDateTime());
            baseMapper.insert(appUser);
            AppUserEntity appUsers = this.lambdaQuery().eq(AppUserEntity::getMobile, phone).one();
            topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID, appUser);//新用户默认加入官方圈子
            saveUserLoginIp(appUsers, request);
            return appUsers.getUid();
        } else {
            if (target.getStatus() == 1) {
                throw new LinfengException("该账号已被禁用");
            }
            this.vipExpirationCheck(target);
            target.setOpenid(param.getWechatOpenId());
            this.saveOrUpdate(target);
            org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + target.getUid());
            saveUserLoginIp(target, request);
            return target.getUid();
        }


    }


    /**
     * 计算两个用户之间的契合度
     * 六维度加权：标签30% + 地域20% + 年龄15% + 兴趣15% + 学历10% + 收入10%
     */
    private void calculateMatchScore(AppUserEntity viewer, AppUserEntity target, AppUserInfoResponse response) {
        List<AppUserInfoResponse.MatchDetail> details = new ArrayList<>();
        double totalScore = 0;

        // 1. 标签匹配 (30%)
        int tagScore = calcTagScore(viewer.getTagStr(), target.getTagStr());
        details.add(new AppUserInfoResponse.MatchDetail("标签匹配", tagScore, buildTagDesc(viewer.getTagStr(), target.getTagStr())));
        totalScore += tagScore * 0.30;

        // 2. 地域匹配 (20%)
        int locationScore = calcLocationScore(viewer, target);
        details.add(new AppUserInfoResponse.MatchDetail("地域相近", locationScore, buildLocationDesc(viewer, target)));
        totalScore += locationScore * 0.20;

        // 3. 年龄匹配 (15%)
        int ageScore = calcAgeScore(viewer.getAge(), target.getAge());
        details.add(new AppUserInfoResponse.MatchDetail("年龄相仿", ageScore, buildAgeDesc(viewer.getAge(), target.getAge())));
        totalScore += ageScore * 0.15;

        // 4. 兴趣匹配 (15%)
        int interestScore = calcInterestScore(viewer.getInterest(), target.getInterest());
        details.add(new AppUserInfoResponse.MatchDetail("兴趣相投", interestScore, buildInterestDesc(viewer.getInterest(), target.getInterest())));
        totalScore += interestScore * 0.15;

        // 5. 学历匹配 (10%)
        int eduScore = calcEducationScore(viewer.getEducation(), target.getEducation());
        details.add(new AppUserInfoResponse.MatchDetail("学历相当", eduScore, ""));
        totalScore += eduScore * 0.10;

        // 6. 收入匹配 (10%)
        int incomeScore = calcIncomeScore(viewer.getIncome(), target.getIncome());
        details.add(new AppUserInfoResponse.MatchDetail("收入相近", incomeScore, ""));
        totalScore += incomeScore * 0.10;

        int finalScore = (int) Math.round(totalScore);
        response.setMatchScore(finalScore);
        response.setMatchDetails(details);
        response.setAiRecommended(finalScore >= 70);
    }

    // ---- 标签维度 ----
    private int calcTagScore(String viewerTagStr, String targetTagStr) {
        Set<String> viewerTags = new HashSet<>(parseTagStr(viewerTagStr));
        Set<String> targetTags = new HashSet<>(parseTagStr(targetTagStr));
        if (viewerTags.isEmpty() || targetTags.isEmpty()) return 30; // 信息缺失给基础分
        Set<String> union = new HashSet<>(viewerTags);
        union.addAll(targetTags);
        Set<String> intersection = new HashSet<>(viewerTags);
        intersection.retainAll(targetTags);
        return union.isEmpty() ? 0 : (int) Math.round(intersection.size() * 100.0 / union.size());
    }

    private String buildTagDesc(String viewerTagStr, String targetTagStr) {
        Set<String> viewerTags = new HashSet<>(parseTagStr(viewerTagStr));
        Set<String> targetTags = new HashSet<>(parseTagStr(targetTagStr));
        Set<String> common = new HashSet<>(viewerTags);
        common.retainAll(targetTags);
        if (common.isEmpty()) return "";
        return "你们都有: " + String.join("、", common);
    }

    // ---- 地域维度 ----
    private int calcLocationScore(AppUserEntity viewer, AppUserEntity target) {
        if (StrUtil.isNotBlank(viewer.getAbodeCity()) && StrUtil.isNotBlank(target.getAbodeCity())
                && viewer.getAbodeCity().equals(target.getAbodeCity())) {
            return 100;
        }
        if (StrUtil.isNotBlank(viewer.getHomeCity()) && StrUtil.isNotBlank(target.getHomeCity())
                && viewer.getHomeCity().equals(target.getHomeCity())) {
            return 60;
        }
        if (StrUtil.isNotBlank(viewer.getProvince()) && StrUtil.isNotBlank(target.getProvince())
                && viewer.getProvince().equals(target.getProvince())) {
            return 30;
        }
        return 0;
    }

    private String buildLocationDesc(AppUserEntity viewer, AppUserEntity target) {
        if (StrUtil.isNotBlank(viewer.getAbodeCity()) && viewer.getAbodeCity().equals(target.getAbodeCity())) {
            return "同在" + viewer.getAbodeCity();
        }
        if (StrUtil.isNotBlank(viewer.getHomeCity()) && viewer.getHomeCity().equals(target.getHomeCity())) {
            return "老家都在" + viewer.getHomeCity();
        }
        if (StrUtil.isNotBlank(viewer.getProvince()) && viewer.getProvince().equals(target.getProvince())) {
            return "同在" + viewer.getProvince();
        }
        return "";
    }

    // ---- 年龄维度 ----
    private int calcAgeScore(Integer viewerAge, Integer targetAge) {
        if (viewerAge == null || targetAge == null) return 50;
        int diff = Math.abs(viewerAge - targetAge);
        if (diff <= 3) return 100;
        if (diff <= 5) return 80;
        if (diff <= 8) return 50;
        if (diff <= 12) return 20;
        return 0;
    }

    private String buildAgeDesc(Integer viewerAge, Integer targetAge) {
        if (viewerAge == null || targetAge == null) return "";
        int diff = Math.abs(viewerAge - targetAge);
        if (diff == 0) return "同龄";
        if (diff <= 3) return "年龄相近";
        return "相差" + diff + "岁";
    }

    // ---- 兴趣维度 ----
    private int calcInterestScore(String viewerInterest, String targetInterest) {
        Set<String> v = parseInterestSet(viewerInterest);
        Set<String> t = parseInterestSet(targetInterest);
        if (v.isEmpty() || t.isEmpty()) return 30;
        Set<String> union = new HashSet<>(v);
        union.addAll(t);
        Set<String> intersection = new HashSet<>(v);
        intersection.retainAll(t);
        return union.isEmpty() ? 0 : (int) Math.round(intersection.size() * 100.0 / union.size());
    }

    private String buildInterestDesc(String viewerInterest, String targetInterest) {
        Set<String> v = parseInterestSet(viewerInterest);
        Set<String> t = parseInterestSet(targetInterest);
        Set<String> common = new HashSet<>(v);
        common.retainAll(t);
        if (common.isEmpty()) return "";
        return "都喜欢" + String.join("、", common);
    }

    private Set<String> parseInterestSet(String interest) {
        if (StrUtil.isBlank(interest)) return Collections.emptySet();
        return Arrays.stream(interest.split("[,，、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    // ---- 学历维度 ----
    private int calcEducationScore(Integer viewerEdu, Integer targetEdu) {
        if (viewerEdu == null || targetEdu == null) return 50;
        int diff = Math.abs(viewerEdu - targetEdu);
        if (diff <= 1) return 100;
        if (diff <= 2) return 60;
        return 20;
    }

    // ---- 收入维度 ----
    private int calcIncomeScore(Integer viewerIncome, Integer targetIncome) {
        if (viewerIncome == null || targetIncome == null) return 50;
        int diff = Math.abs(viewerIncome - targetIncome);
        if (diff == 0) return 100;
        if (diff == 1) return 80;
        if (diff == 2) return 40;
        return 10;
    }

    /**
     * 解析 tagStr 字符串为 List，兼容 JSON 格式 ["a","b"] 和 Java List.toString 格式 [a, b]
     */
    private List<String> parseTagStr(String tagStr) {
        if (org.apache.commons.lang3.StringUtils.isBlank(tagStr)) {
            return Collections.emptyList();
        }
        String raw = tagStr.trim();
        // 尝试 JSON 数组解析
        if (raw.startsWith("[")) {
            try {
                List<String> list = JSON.parseArray(raw, String.class);
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            } catch (Exception ignored) {
                // 非 JSON 格式，走下面的分割逻辑
            }
            // 去掉两端括号后按逗号分割
            raw = raw.substring(1, raw.length() - 1);
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    @DSTransactional
    public void cancelAccount(AppUserEntity user, String reason) {
        AppUserEntity current = this.getById(user.getUid());
        if (current == null) {
            throw new LinfengException("账号不存在");
        }
        if (current.getStatus() != null && current.getStatus() == 1) {
            throw new LinfengException("当前账号已不可用");
        }

        Integer uid = current.getUid();
        current.setStatus(1);
        current.setUsername("已注销用户" + uid);
        current.setMobile(null);
        current.setEmail(null);
        current.setPassword(RandomUtil.randomString(24));
        current.setAvatar(Constant.DEAULT_HEAD);
        current.setOpenid(null);
        current.setMpOpenid(null);
        current.setUnionid(null);
        current.setProvince(null);
        current.setCity(null);
        current.setHomeCity(null);
        current.setAbodeCity(null);
        current.setLocationCity(null);
        current.setLocationUpdateTime(null);
        current.setIntro("账号已注销");
        current.setTagStr("[]");
        current.setInfo(null);
        current.setSelfIntroduction(null);
        current.setLoveDeclaration(null);
        current.setInterest(null);
        current.setAdminreHerart(null);
        current.setSchool(null);
        current.setEduCode(null);
        current.setIdentyCode(null);
        current.setBirthday(null);
        current.setLastLoginIp(null);
        current.setAuditStatus(0);
        current.setIdentyCertifStatus(0);
        current.setEduCertifStatus(0);
        current.setUpdateTime(DateUtil.nowDateTime());

        if (!this.updateById(current)) {
            throw new LinfengException("注销失败，请稍后重试");
        }

        userSettingService.remove(Wrappers.<UserSettingEntity>lambdaQuery().eq(UserSettingEntity::getUid, uid));
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + uid);
        log.info("user cancelAccount success uid={}, reason={}", uid, StrUtil.blankToDefault(reason, "none"));
    }

}
