package org.aileme.shejiao.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.common.utils.RedisKeys;
import org.aileme.shejiao.domain.entity.sys.SysConfigEntity;

/**
 * 系统配置Redis
 *
 */
@Component
public class SysConfigRedis {
    public void saveOrUpdate(SysConfigEntity config) {
        if(config == null){
            return ;
        }
        String key = RedisKeys.getSysConfigKey(config.getParamKey());
        org.aileme.common.redis.utils.RedisUtils.setCacheObject(key, config);
    }

    public void delete(String configKey) {
        String key = RedisKeys.getSysConfigKey(configKey);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(key);
    }

    public SysConfigEntity get(String configKey){
        String key = RedisKeys.getSysConfigKey(configKey);
        return org.aileme.common.redis.utils.RedisUtils.getCacheObject(key);
    }

}
