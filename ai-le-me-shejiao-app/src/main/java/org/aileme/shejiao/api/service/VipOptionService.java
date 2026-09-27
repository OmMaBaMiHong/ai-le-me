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
package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.OrderPay;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;
import org.aileme.shejiao.domain.entity.admin.VipOptionEntity;
import org.aileme.shejiao.domain.param.app.VipPayForm;
import org.aileme.shejiao.domain.param.app.VipRechargeForm;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 会员充值选项
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-09-28 14:26:17
 */
public interface VipOptionService extends IService<VipOptionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    String rechargeVip(AppUserEntity user,VipRechargeForm param);

    OrderPay vipPay(AppUserEntity user, VipPayForm param, HttpServletRequest request) throws Exception;

    void rollBackVip(UserRechargeEntity userRecharge);
}

