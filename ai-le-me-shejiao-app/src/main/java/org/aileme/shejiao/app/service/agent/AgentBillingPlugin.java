package org.aileme.shejiao.app.service.agent;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

public interface AgentBillingPlugin {

    String modeCode();

    boolean isBillingEnabled(String sceneCode);

    void ensureSufficientBudget(AppUserEntity user, String sceneCode, int inputChars);

    void settleUsage(AppUserEntity user, Integer targetUid, String sceneCode, int inputChars, String referenceId);

    int estimateCoin(String sceneCode, int inputChars);

    int getVipDailyFreeQuota(String sceneCode);

    int getVipDailyFreeRemaining(AppUserEntity user, String sceneCode);
}
