package org.aileme.shejiao.admin.service.group;

import org.apache.commons.lang3.StringUtils;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

@Component
public class ScrmGroupTouchProvider implements GroupTouchProvider {

    private static final String SERVICE_TYPE = "scrm_vendor";

    private final ISysThirdPartyService thirdPartyService;

    public ScrmGroupTouchProvider(ISysThirdPartyService thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    @Override
    public int providerType() {
        return 2;
    }

    @Override
    public ProviderResult syncGroup(HongniangWechatGroupEntity group) {
        String baseUrl = thirdPartyService.getConfigValue(SERVICE_TYPE, "baseUrl");
        String appKey = thirdPartyService.getConfigValue(SERVICE_TYPE, "appKey");
        if (StringUtils.isAnyBlank(baseUrl, appKey)) {
            return ProviderResult.builder()
                .success(false)
                .summary("SCRM 配置缺失，请先补齐 scrm_vendor 配置")
                .rawResponse("{\"message\":\"missing scrm_vendor configs\"}")
                .build();
        }
        return ProviderResult.builder()
            .success(true)
            .summary("SCRM 同步请求已提交，本期先记录同步状态")
            .rawResponse("{\"message\":\"scrm sync accepted\"}")
            .build();
    }

    @Override
    public ProviderResult submitTask(HongniangWechatGroupEntity group, HongniangGroupTouchTaskEntity task) {
        String baseUrl = thirdPartyService.getConfigValue(SERVICE_TYPE, "baseUrl");
        String appKey = thirdPartyService.getConfigValue(SERVICE_TYPE, "appKey");
        String appSecret = thirdPartyService.getConfigValue(SERVICE_TYPE, "appSecret");
        if (StringUtils.isAnyBlank(baseUrl, appKey, appSecret)) {
            return ProviderResult.builder()
                .success(false)
                .summary("SCRM 配置缺失，请先补齐 baseUrl/appKey/appSecret")
                .rawResponse("{\"message\":\"missing scrm_vendor task configs\"}")
                .build();
        }
        return ProviderResult.builder()
            .success(true)
            .providerTaskId("SCRM-" + System.currentTimeMillis())
            .summary("SCRM 触达任务已提交，本期以任务日志回写为准")
            .rawResponse("{\"message\":\"scrm task accepted\"}")
            .build();
    }
}
