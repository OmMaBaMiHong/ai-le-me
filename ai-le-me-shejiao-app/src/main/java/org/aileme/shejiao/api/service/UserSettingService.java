/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.vo.AppUserSettingResponse;
import org.aileme.shejiao.domain.entity.app.UserSettingEntity;
import org.aileme.shejiao.domain.param.app.UpdateUserSettingForm;

import java.util.Map;

/**
 * 用户隐私设置
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-24 15:17:15
 */
public interface UserSettingService extends IService<UserSettingEntity> {

    PageUtils queryPage(Map<String, Object> params);

    AppUserSettingResponse userSetting(Integer uid);

    void updateUserSetting(Integer uid, UpdateUserSettingForm param);
}

