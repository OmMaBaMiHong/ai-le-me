package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.VipBenefitEntity;

import java.util.List;
import java.util.Map;

/**
 * VIP会员权益设置
 *
 * @author Wade
 * @date 2026-02-10
 */
public interface VipBenefitService extends IService<VipBenefitEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<VipBenefitEntity> getList();
}
