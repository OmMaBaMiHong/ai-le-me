package org.aileme.shejiao.app.service.agent;

import java.time.Duration;

public interface AgentCompanionQuotaStore {

    int getInt(String key);

    int increment(String key, Duration ttl);
}
