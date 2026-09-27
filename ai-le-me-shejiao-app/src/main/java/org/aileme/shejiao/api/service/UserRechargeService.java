package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.OrderPay;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.RechargeEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeRefundEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;
import org.aileme.shejiao.domain.param.app.PayVipPostForm;
import org.aileme.shejiao.common.utils.AppPageUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户充值
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-19 19:27:33
 */
public interface UserRechargeService extends IService<UserRechargeEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 添加充值记录
     * @param user 用户
     * @param price 充值金额
     * @param paidPrice 赠送金额
     * @Param type 支付类型
     */
    String addRecharge(AppUserEntity user, String price, String paidPrice,String type);

    String addRecharge(AppUserEntity user, String price, String paidPrice, String payType, Integer orderType);

    String addRecharge(AppUserEntity user, RechargeEntity product, String payType, String payChannel, Integer orderType);

    String addActivityOrder(AppUserEntity user, XiangqinEnrollmentEntity enrollment, XiangqinActivityEntity activity,
                            String payType, String payChannel);

    OrderPay goToPay(String orderSn, Double price,String ip,AppUserEntity user,String type) throws Exception;

    OrderPay goToPay(String orderSn, Double price, String ip, AppUserEntity user, String type, String payChannel, String h5Origin) throws Exception;

    void handleWxLog(String orderId, String transaction_id, String orderNo);

    void handleMockPay(String orderId, String payChannel, String status);

    void payVipPost(PayVipPostForm param,AppUserEntity user);

    double rechargeMoney();

    double rechargeMoneyByMonth();

    AppPageUtils coinRechargePage(Integer uid, Integer page, Integer limit);

    void refundOrder(String orderId, BigDecimal refundAmount, String reason, String operatorName);

    List<UserRechargeRefundEntity> refundList(String orderId);
}
