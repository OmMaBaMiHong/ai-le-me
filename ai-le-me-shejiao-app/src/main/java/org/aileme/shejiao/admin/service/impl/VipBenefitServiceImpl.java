package org.aileme.shejiao.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.aileme.common.redis.utils.RedisUtils;
import org.aileme.shejiao.app.utils.WechatUtil;
import org.aileme.shejiao.common.utils.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.aileme.shejiao.admin.dao.VipBenefitDao;
import org.aileme.shejiao.domain.entity.admin.VipBenefitEntity;
import org.aileme.shejiao.api.service.VipBenefitService;


@DS("master")
@Service("vipBenefitService")
public class VipBenefitServiceImpl extends ServiceImpl<VipBenefitDao, VipBenefitEntity> implements VipBenefitService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<VipBenefitEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(VipBenefitEntity::getSort);
        IPage<VipBenefitEntity> page = this.page(
            new Query<VipBenefitEntity>().getPage(params),
            queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<VipBenefitEntity> getList() {
        String result = RedisUtils.getCacheObject(ConfigConstant.VIP_BENEFIT_KEY);
        if(WechatUtil.isEmpty(result)){
            List<VipBenefitEntity> list = this.lambdaQuery()
                .eq(VipBenefitEntity::getStatus,Constant.NORMAL)
                .orderByDesc(VipBenefitEntity::getSort).list();
            RedisUtils.setCacheObject(ConfigConstant.VIP_BENEFIT_KEY, JSON.toJSON(list).toString(), Duration.ZERO.plusDays(1));
            return list;
        }
        return JSONObject.parseArray(result, VipBenefitEntity.class);
    }

}
