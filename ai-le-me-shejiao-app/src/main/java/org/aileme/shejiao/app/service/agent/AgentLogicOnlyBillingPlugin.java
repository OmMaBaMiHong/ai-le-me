package org.aileme.shejiao.app.service.agent;

import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

@Service
public class AgentLogicOnlyBillingPlugin implements AgentBillingPlugin {

    @Override
    public String modeCode() {
        return "logic_only";
    }

    @Override
    public boolean isBillingEnabled(String sceneCode) {
        return false;
    }

    @Override
    public void ensureSufficientBudget(AppUserEntity user, String sceneCode, int inputChars) {
    }

    @Override
    public void settleUsage(AppUserEntity user, Integer targetUid, String sceneCode, int inputChars, String referenceId) {
    }

    @Override
    public int estimateCoin(String sceneCode, int inputChars) {
        return 0;
    }

    @Override
    public int getVipDailyFreeQuota(String sceneCode) {
        return 0;
    }

    @Override
    public int getVipDailyFreeRemaining(AppUserEntity user, String sceneCode) {
        return 0;
    }
}
