package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.PayOrderDetailEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;

import java.math.BigDecimal;

public interface PayOrderDetailService extends IService<PayOrderDetailEntity> {

    void record(UserRechargeEntity order, String eventType, String title, BigDecimal amount, Integer coinAmount, String remark);
}
