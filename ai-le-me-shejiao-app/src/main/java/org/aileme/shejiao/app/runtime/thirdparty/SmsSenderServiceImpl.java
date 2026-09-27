package org.aileme.shejiao.app.runtime.thirdparty;

import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SmsSenderService;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;
import org.aileme.shejiao.common.exception.LinfengException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class SmsSenderServiceImpl implements SmsSenderService {

    private static final String SERVICE_SMS = "sms";
    private static final String PROVIDER_ALIYUN = "aliyun";

    private final ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService;

    public SmsSenderServiceImpl(ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService) {
        this.thirdPartyRuntimeConfigService = thirdPartyRuntimeConfigService;
    }

    @Override
    public void sendLoginCode(String mobile, String code) {
        String providerCode = thirdPartyRuntimeConfigService.getProviderCode(SERVICE_SMS, PROVIDER_ALIYUN);
        if (!PROVIDER_ALIYUN.equalsIgnoreCase(providerCode)) {
            throw new LinfengException("当前短信渠道暂未接入发送能力，请切换为 aliyun");
        }
        sendAliyunTemplate(mobile, resolveAliyunTemplateCode(), buildTemplateParams(code));
    }

    private void sendAliyunTemplate(String mobile, String templateCode, Map<String, Object> templateParams) {
        String regionId = requireConfig("阿里云短信 RegionId", "region_id", "regionId");
        String accessKeyId = requireConfig("阿里云短信 AccessKeyId", "access_key_id", "accessKeyId");
        String accessKeySecret = requireConfig("阿里云短信 AccessKeySecret", "access_key_secret", "accessKeySecret");
        String signName = requireConfig("阿里云短信签名", "sign_name", "signName");

        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        request.putQueryParameter("RegionId", regionId);
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", signName);
        request.putQueryParameter("TemplateCode", templateCode);
        request.putQueryParameter("TemplateParam", JSON.toJSONString(templateParams));

        try {
            CommonResponse response = client.getCommonResponse(request);
            log.info("短信发送响应: {}", response.getData());
            if (!response.getHttpResponse().isSuccess() || StringUtils.containsIgnoreCase(response.getData(), "\"Code\":\"isv.")) {
                throw new LinfengException("短信发送失败，请检查 aliyun 短信配置");
            }
        } catch (Exception e) {
            if (e instanceof LinfengException linfengException) {
                throw linfengException;
            }
            throw new LinfengException("短信发送失败: " + e.getMessage());
        }
    }

    private String resolveAliyunTemplateCode() {
        return requireConfig("阿里云登录验证码模板", "template_login", "template_code", "templateCode");
    }

    private Map<String, Object> buildTemplateParams(String code) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("code", code);
        return params;
    }

    private String requireConfig(String label, String... candidateKeys) {
        String value = thirdPartyRuntimeConfigService.getConfig(SERVICE_SMS, PROVIDER_ALIYUN, candidateKeys);
        if (StringUtils.isBlank(value)) {
            throw new LinfengException(label + "未配置，请先补齐 sms/aliyun 三方配置");
        }
        return value;
    }
}
