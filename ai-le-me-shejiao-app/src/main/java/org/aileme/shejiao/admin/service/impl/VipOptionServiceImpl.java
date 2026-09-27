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

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.vo.OrderPay;
import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.PayOrderDetailService;
import org.aileme.shejiao.api.service.UserRechargeService;
import org.aileme.shejiao.domain.param.app.VipPayForm;
import org.aileme.shejiao.domain.param.app.VipRechargeForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.VipOptionDao;
import org.aileme.shejiao.domain.entity.admin.VipOptionEntity;
import org.aileme.shejiao.api.service.VipOptionService;

import jakarta.servlet.http.HttpServletRequest;


@DS("master")
@Service("vipOptionService")
public class VipOptionServiceImpl extends ServiceImpl<VipOptionDao, VipOptionEntity> implements VipOptionService {

    @Lazy
    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private AppUserService userService;

    @Autowired
    private BillService billService;
    @Autowired
    private PayOrderDetailService payOrderDetailService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<VipOptionEntity> page = this.page(
                new Query<VipOptionEntity>().getPage(params),
                new QueryWrapper<VipOptionEntity>().lambda()
                        .eq(VipOptionEntity::getProductType, Constant.PAY_PRODUCT_TYPE_VIP)
                        .orderByAsc(VipOptionEntity::getSort)
        );

        return new PageUtils(page);
    }

    @Override
    public String rechargeVip(AppUserEntity user,VipRechargeForm param) {
        VipOptionEntity vipOption = this.getById(param.getVipId());
        if(ObjectUtil.isNull(vipOption)){
            throw new LinfengException("充值方案不存在");
        }
        UserRechargeEntity entity=new UserRechargeEntity();
        String orderSn = IdUtil.createSnowflake(0L, 0).nextIdStr();
        entity.setNickname(user.getUsername());
        entity.setOrderId(orderSn);
        entity.setTitle(vipOption.getName());
        entity.setUid(user.getUid());
        entity.setBizId(String.valueOf(vipOption.getId()));
        entity.setPrice(vipOption.getPrice());
        entity.setGivePrice(BigDecimal.ZERO);
        entity.setCoinAmount(0);
        entity.setRechargeType(param.getPayType());
        entity.setChannel(param.getPayChannel());
        entity.setStatus(Constant.RECHARGE_STATUS_PENDING);
        entity.setAddTime(DateUtil.nowDateTime());
        entity.setUpdateTime(DateUtil.nowDateTime());
        entity.setType(Constant.RECHARGE_ORDER_VIP);
        userRechargeService.save(entity);
        payOrderDetailService.record(entity, Constant.BILL_EVENT_ORDER_CREATE, "订单创建", vipOption.getPrice(), 0,
                "订单创建，" + vipOption.getName());
        return orderSn;
    }

    @Override
    public OrderPay vipPay(AppUserEntity user, VipPayForm param, HttpServletRequest request) throws Exception {
        UserRechargeEntity userRecharge = userRechargeService.lambdaQuery().eq(UserRechargeEntity::getOrderId, param.getOrderId()).one();
        if(ObjectUtil.isNull(userRecharge)){
            throw new LinfengException("充值订单不存在");
        }
        String ip= AppletPayUtil.getClientIp(request);
        String h5Origin = null;
        if (Constant.PAY_TYPE_H5.equals(param.getPayType())) {
            String origin = request.getHeader("Origin");
            if (ObjectUtil.isNull(origin) || origin.isBlank()) {
                String referer = request.getHeader("Referer");
                if (ObjectUtil.isNotNull(referer) && !referer.isBlank()) {
                    int index = referer.indexOf("/#");
                    origin = index > 0 ? referer.substring(0, index) : referer;
                }
            }
            if (ObjectUtil.isNotNull(origin) && origin.endsWith("/")) {
                origin = origin.substring(0, origin.length() - 1);
            }
            h5Origin = origin;
        }
        OrderPay orderPay=userRechargeService.goToPay(userRecharge.getOrderId(),userRecharge.getPrice().doubleValue(),ip,user,
                param.getPayType(), param.getPayChannel(), h5Origin);
        return orderPay;
    }

    /**
     * 会员支付回调业务
     * 更新用户会员信息 添加账单记录
     * @param userRecharge
     */
    @Override
    public void rollBackVip(UserRechargeEntity userRecharge) {
        AppUserEntity user = userService.getById(userRecharge.getUid());
        int vipOptionId = Integer.parseInt(userRecharge.getBizId());
        VipOptionEntity vipOption= this.getById(vipOptionId);
        if(user.getVip().equals(Constant.VIP_USER) && ObjectUtil.isNotNull(user.getVipExpireTime())){
            //续费用户
            String toStr = DateUtil.dateToStr(user.getVipExpireTime(), "yyyy-MM-dd HH:mm:ss");
            user.setVipExpireTime(DateUtil.addDay(toStr,vipOption.getValidDays()));
        }else{
            user.setVip(Constant.VIP_USER);
            user.setVipExpireTime(DateUtil.addDay(DateUtil.nowDateTimeStr(),vipOption.getValidDays()));
        }
        boolean b = userService.saveOrUpdate(user);
        if(!b){
            throw new LinfengException("会员信息更新失败");
        }
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" +user.getUid());
    }

}
