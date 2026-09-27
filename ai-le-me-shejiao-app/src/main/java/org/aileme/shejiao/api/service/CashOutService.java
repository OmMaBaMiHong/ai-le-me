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
package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.vo.AppCashInfoResponse;
import org.aileme.shejiao.domain.entity.admin.CashOutEntity;
import org.aileme.shejiao.domain.param.app.AddCashOutForm;

import java.util.Map;

/**
 * 提现
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-01 11:43:29
 */
public interface CashOutService extends IService<CashOutEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void submit(AddCashOutForm param,Integer uid);

    boolean checkIsNormal(Integer uid);

    AppCashInfoResponse getAccountBasicInfo(Integer uid);

    void updateCash(CashOutEntity cashOut);
}

