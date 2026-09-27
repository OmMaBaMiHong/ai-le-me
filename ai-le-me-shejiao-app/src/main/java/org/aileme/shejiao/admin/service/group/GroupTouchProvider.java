package org.aileme.shejiao.admin.service.group;

import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

public interface GroupTouchProvider {

    int providerType();

    ProviderResult syncGroup(HongniangWechatGroupEntity group);

    ProviderResult submitTask(HongniangWechatGroupEntity group, HongniangGroupTouchTaskEntity task);
}
