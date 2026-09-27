package org.aileme.shejiao.app.service.agent;

import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisAgentCompanionQuotaStore implements AgentCompanionQuotaStore {

    @Override
    public int getInt(String key) {
        Object cached = RedisUtils.getCacheObject(key);
        if (cached == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(cached));
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public int increment(String key, Duration ttl) {
        int next = getInt(key) + 1;
        RedisUtils.setCacheObject(key, String.valueOf(next), ttl);
        return next;
    }
}
