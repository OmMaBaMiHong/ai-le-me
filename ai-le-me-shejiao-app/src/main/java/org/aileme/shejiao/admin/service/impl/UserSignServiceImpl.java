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
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.SignResponse;
import org.aileme.shejiao.domain.vo.SignUserResponse;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.BillEntity;
import org.aileme.shejiao.domain.entity.admin.SignConfigEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.SignConfigService;
import org.aileme.shejiao.domain.param.app.IntegralTypeEnum;
import org.aileme.shejiao.api.service.SysConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.UserSignDao;
import org.aileme.shejiao.domain.entity.admin.UserSignEntity;
import org.aileme.shejiao.api.service.UserSignService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;


@DS("master")
@Service("userSignService")
public class UserSignServiceImpl extends ServiceImpl<UserSignDao, UserSignEntity> implements UserSignService {


    @Autowired
    private UserSignDao userSignDao;
    @Autowired
    private AppUserService userService;
    @Autowired
    private BillService billService;
    @Autowired
    private SignConfigService signConfigService;
    @Autowired
    private SysConfigService configService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<UserSignEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().orderByDesc(UserSignEntity::getId);
        IPage<UserSignEntity> page = this.page(
                new Query<UserSignEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 获取签到用户信息
     * @param appUser
     * @return
     */
    @Override
    public SignUserResponse getUserInfo(AppUserEntity appUser) {
        AppUserEntity currentUser = userService.getById(appUser.getUid());
        AppUserEntity user = userService.vipExpirationCheck(currentUser);
        SignUserResponse vo = new SignUserResponse();
        BeanUtils.copyProperties(user, vo);
        int sumSignDay = this.getSignSumDay(user.getUid());
        boolean isDaySign = this.getToDayIsSign(user.getUid());
        boolean isYestDaySign = this.getYestDayIsSign(user.getUid());
        vo.setSumSignDay(sumSignDay);
        vo.setIsDaySign(isDaySign);
        vo.setIsYestDaySign(isYestDaySign);
        if (!isDaySign && !isYestDaySign) {
            vo.setSignNum(0);
        }
        return vo;
    }

    @Override
    public List<SignResponse> getSignList(Integer uid, Integer page, Integer limit) {
        Page<BillEntity> pageModel = new Page<>(page, limit);
        return userSignDao.getSignList(uid, pageModel);
    }


    @Override
    @DSTransactional
    public int sign(AppUserEntity appUser) {
        AppUserEntity user = userService.getById(appUser.getUid());
        boolean isDaySign = this.getToDayIsSign(user.getUid());
        if (isDaySign) {
            throw new LinfengException("签到过啦");
        }
        List<SignConfigEntity> list = this.getConfigList();
        int signNumber = 0; //积分
        int userSignNum = user.getSignNum(); //签到次数
        if (this.getYestDayIsSign(user.getUid())) {
            if (user.getSignNum() > (list.size() - 1)) {
                userSignNum = 0;
            }
        } else {
            userSignNum = 0;
        }
        int index = 0;
        for (SignConfigEntity response : list) {
            if (index == userSignNum) {
                signNumber = Integer.valueOf(response.getSignNum());
                break;
            }
            index++;
        }
        userSignNum += 1;
        //会员签到积分翻倍奖励


        if (user.getVip().equals(Constant.VIP_USER)) {
            String value = configService.getValue(Constant.VIP_INTEGRAL);
            Integer multiple = Integer.valueOf(value);
            if (multiple > 0) {
                signNumber = signNumber * multiple;
            }
        }
        //积分信息新增
        UserSignEntity userSign = new UserSignEntity();
        userSign.setUid(user.getUid());
        String title = "签到奖励";
        if (userSignNum == list.size()) {
            title = "连续签到奖励";
        }
        userSign.setTitle(title);
        userSign.setNumber(signNumber);
        userSign.setBalance(user.getIntegral());
        userSign.setCreateTime(org.aileme.shejiao.common.utils.DateUtil.nowDateTime());
        userSignDao.insert(userSign);
        //更新用户积分信息
        boolean update = userService.lambdaUpdate()
                .set(AppUserEntity::getIntegral, user.getIntegral() + signNumber)
                .set(AppUserEntity::getSignNum, userSignNum)
                .eq(AppUserEntity::getUid, user.getUid())
                .update();
        if (!update) {
            throw new LinfengException("用户积分更新失败");
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" + user.getUid());
        //积分账单插入
        billService.income(user.getUid(), title, BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_10.getValue(), signNumber, user.getIntegral().doubleValue(),
                "签到积分奖励", "", null);
        return signNumber;
    }

    @Override
    public List<SignConfigEntity> getConfigList() {
        return signConfigService.lambdaQuery()
                .orderByAsc(SignConfigEntity::getSort)
                .list();
    }

    /**
     * 今天签到用户数
     * @return
     */
    @Override
    public Integer getSignUserCount() {
        Date today = DateUtil.beginOfDay(new Date());
        return Math.toIntExact(this.lambdaQuery()
                .ge(UserSignEntity::getCreateTime, today)
                .count());
    }

    @Override
    public String consumeIntegral(AppUserEntity appUserEntity, Integer type) {
        AppUserEntity user = userService.getById(appUserEntity.getUid());
        if(user!=null && user.getIntegral()>0){
            IntegralTypeEnum integralTypeEnum=  IntegralTypeEnum.getBytype(type);
            switch (integralTypeEnum){
                case NOTE_TYPE:{
                    if(user.getIntegral()>=IntegralTypeEnum.NOTE_TYPE.getIntegray()){
                        user.setIntegral(user.getIntegral()-IntegralTypeEnum.NOTE_TYPE.getIntegray());
                        userService.updateById(user);
                        return  "";
                    }else{
                        return  "您的积分不够啦";
                    }
                }
                default: ;
            }
        }
        return "您的积分不够啦";
    }

    /**
     * 用户累计签到次数
     * @param uid 用户id
     * @return int
     */
    private int getSignSumDay(Integer uid) {
        return Math.toIntExact(this.lambdaQuery().eq(UserSignEntity::getUid, uid).count());
    }

    /**
     * 用户昨天是否签到
     * @param uid uid
     * @return boolean
     */
    private boolean getYestDayIsSign(Integer uid) {
        Date today = DateUtil.beginOfDay(new Date());
        Date yesterday = DateUtil.beginOfDay(DateUtil.yesterday());

        int count = Math.toIntExact(this.lambdaQuery().eq(UserSignEntity::getUid, uid)
                .lt(UserSignEntity::getCreateTime, today)
                .ge(UserSignEntity::getCreateTime, yesterday)
                .count());
        return count > 0;
    }

    /**
     * 用户今天是否签到
     * @param uid uid
     * @return boolean
     */
    private boolean getToDayIsSign(Integer uid) {
        Date today = DateUtil.beginOfDay(new Date());
        int count = Math.toIntExact(this.lambdaQuery().eq(UserSignEntity::getUid, uid)
                .ge(UserSignEntity::getCreateTime, today)
                .count());
        return count > 0;
    }

}
