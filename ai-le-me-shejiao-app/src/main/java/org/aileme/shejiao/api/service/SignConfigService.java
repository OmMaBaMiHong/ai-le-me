package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.SignConfigEntity;

import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 20:28:46
 */
public interface SignConfigService extends IService<SignConfigEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

