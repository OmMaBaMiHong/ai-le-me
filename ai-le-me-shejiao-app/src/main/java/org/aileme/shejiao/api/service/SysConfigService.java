package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.sys.SysConfigEntity;

import java.util.Map;

/**
 * 系统配置信息
 *
 */
public interface SysConfigService {


	/**
	 * 根据key，获取配置的value值
	 */
	String getValue(String key);


}
