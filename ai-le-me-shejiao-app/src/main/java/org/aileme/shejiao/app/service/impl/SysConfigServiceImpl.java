package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.api.service.SysConfigService;

@DS("master")
@Service("sheJiaoSysConfigServiceImpl")
public class SysConfigServiceImpl  implements SysConfigService {

    private final PlatformBusinessConfigService businessConfigService;

    public SysConfigServiceImpl(PlatformBusinessConfigService businessConfigService) {
        this.businessConfigService = businessConfigService;
    }

	/**
	 * 获取配置值
	 * @param key
	 * @return
	 */
	@Override
	public String getValue(String key) {
        return businessConfigService.getString(key);
	}

}
