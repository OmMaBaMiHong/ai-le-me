package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.PayOrderDetailDao;
import org.aileme.shejiao.api.service.PayOrderDetailService;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.domain.entity.admin.PayOrderDetailEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;

import java.math.BigDecimal;

@DS("master")
@Service("payOrderDetailService")
public class PayOrderDetailServiceImpl extends ServiceImpl<PayOrderDetailDao, PayOrderDetailEntity> implements PayOrderDetailService {

    @Override
    public void record(UserRechargeEntity order, String eventType, String title, BigDecimal amount, Integer coinAmount, String remark) {
        PayOrderDetailEntity detail = new PayOrderDetailEntity();
        detail.setOrderRecordId(order.getId());
        detail.setOrderId(order.getOrderId());
        detail.setUid(order.getUid());
        detail.setOrderType(order.getType());
        detail.setEventType(eventType);
        detail.setTitle(title);
        detail.setAmount(amount == null ? BigDecimal.ZERO : amount);
        detail.setCoinAmount(coinAmount == null ? 0 : coinAmount);
        detail.setBizId(order.getBizId());
        detail.setTransactionId(order.getTransactionId());
        detail.setRemark(remark);
        detail.setStatus(1);
        detail.setAddTime(DateUtil.nowDateTime());
        this.save(detail);
    }
}
