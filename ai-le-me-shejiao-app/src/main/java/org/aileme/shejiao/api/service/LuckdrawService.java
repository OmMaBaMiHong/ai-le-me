package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.LuckdrawEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-08-14 14:28:48
 */
public interface LuckdrawService extends IService<LuckdrawEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Map<String,Object> getPrize(AppUserEntity user);

    Map<String,Object> startLuckDraw(AppUserEntity user);

    AppPageUtils record(AppUserEntity user,Integer page);
}

