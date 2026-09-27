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
import org.aileme.shejiao.domain.entity.app.MomentEntity;

import java.util.Map;

/**
 * 动态表
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-05 14:23:11
 */
public interface MomentService extends IService<MomentEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

