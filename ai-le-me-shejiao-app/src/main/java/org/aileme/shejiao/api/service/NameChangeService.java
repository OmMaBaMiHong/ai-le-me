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
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.NameChangeEntity;

import java.util.Map;

/**
 * 用户名称修改
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-10-07 12:40:50
 */
public interface NameChangeService extends IService<NameChangeEntity> {

    PageUtils queryPage(Map<String, Object> params);

    boolean canChangeName(AppUserEntity user);

}

