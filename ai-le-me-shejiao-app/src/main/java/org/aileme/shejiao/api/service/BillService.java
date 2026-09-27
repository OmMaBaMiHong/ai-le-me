package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.BillEntity;
import org.aileme.shejiao.domain.param.app.AddRewardForm;
import org.aileme.shejiao.domain.param.app.ExchangeForm;
import org.aileme.shejiao.domain.param.app.getBillListForm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户账单表
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-20 20:46:48
 */
public interface BillService extends IService<BillEntity> {

    PageUtils queryPage(Map<String, Object> params);


    void expend(Integer uid,String title,String category,String type,double number,double balance,String mark,String linkId,Integer tipUserId);


    void income(Integer uid,String title,String category,String type,double number,
                double balance,String mark,String linkid,Integer tipUserId);

    void record(Integer uid, Integer pm, String title, String category, String type, double number,
                double balance, String mark, String linkId, String orderId, Integer tipUserId, Integer status);

    boolean vipPostIsPay(Integer postId,Integer userId);

    AppPageUtils billList(getBillListForm request, AppUserEntity user);

    BigDecimal getAllPay(Integer userId);

    List<BillEntity> getIntegralList(Integer uid, Integer page, Integer limit,Integer type);

    Integer getUsedIntegral(Integer uid);

    void exchange(AppUserEntity user, ExchangeForm request);

    void exchangeMoneyToIntegral(AppUserEntity user, ExchangeForm request);

    void rewardIntegral(AppUserEntity user, AddRewardForm request);

    /**
     * 获取帖子打赏列表
     * @param postId 帖子ID
     * @return 打赏用户列表
     */
    List<Map<String, Object>> getPostRewardList(Integer postId);

    /**
     * 获取帖子打赏总积分
     * @param postId 帖子ID
     * @return 总积分
     */
    Integer getPostRewardTotal(Integer postId);
}
