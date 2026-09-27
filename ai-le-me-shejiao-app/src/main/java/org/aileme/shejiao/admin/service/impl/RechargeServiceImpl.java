/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.common.utils.Constant;

import org.aileme.shejiao.admin.dao.RechargeDao;
import org.aileme.shejiao.domain.entity.admin.RechargeEntity;
import org.aileme.shejiao.api.service.RechargeService;


@DS("master")
@Service("rechargeService")
public class RechargeServiceImpl extends ServiceImpl<RechargeDao, RechargeEntity> implements RechargeService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<RechargeEntity> page = this.page(
                new Query<RechargeEntity>().getPage(params),
                new LambdaQueryWrapper<RechargeEntity>()
                        .eq(RechargeEntity::getProductType, Constant.PAY_PRODUCT_TYPE_COIN)
                        .orderByDesc(RechargeEntity::getSort)
        );

        return new PageUtils(page);
    }

    @Override
    public List<RechargeEntity> getAllRecharge() {
        return this.lambdaQuery()
                .eq(RechargeEntity::getProductType, Constant.PAY_PRODUCT_TYPE_COIN)
                .eq(RechargeEntity::getStatus, 0)
                .orderByDesc(RechargeEntity::getSort)
                .list();
    }

}
