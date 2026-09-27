package org.aileme.shejiao.app.biz;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.WxConfigConstant;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class WeChatBiz {
    @Autowired
    private WxMpService wxMpService;

    @Autowired
    private PlatformBusinessConfigService businessConfigService;


    public void returnVerficationCode(String receiveId) {
        ensureWechatConfigured();
        String templateId = resolveVerificationTemplateId();
        //模板消息封装的对象
        WxMpTemplateMessage wxMpTemplateMessage = new WxMpTemplateMessage();
        //消息模板ID
        wxMpTemplateMessage.setTemplateId(templateId);
        wxMpTemplateMessage.setToUser(receiveId);
        wxMpTemplateMessage.setData(wrapperTemplateData());
        try {
            wxMpService.getTemplateMsgService().sendTemplateMsg(wxMpTemplateMessage);
        }catch (Exception errorException){
            log.error("推送出现错误！" );
        }
    }

    /**
     *  得到验证码封装数据
     * @return
     */
    private List<WxMpTemplateData> wrapperTemplateData(){
        //得到4为验证码
        String code = getVerficationCode(4);
        String validityTime = StrUtil.firstNonBlank(
                businessConfigService.getString(Constant.WX_MP_VERIFICATION_VALIDITY_TIME),
                WxConfigConstant.VERFICATION_CODE_VALIDITY_TIME,
                "10分钟"
        );
        List<WxMpTemplateData> wxMpTemplateData = new ArrayList<>();
        wxMpTemplateData.add(new WxMpTemplateData("code",code));
        wxMpTemplateData.add(new WxMpTemplateData("validity", validityTime));
        return wxMpTemplateData;
    }
    private static final String SYMBOLS = "0123456789"; // 数字
    private static final Random RANDOM = new SecureRandom();

    /**
     *  生成指定位数的数字验证码
     * @return
     */
    public static String getVerficationCode(int length) {

        // 如果需要4位，那 new char[4] 即可，其他位数同理可得
        char[] nonceChars = new char[length];

        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        }
        return new String(nonceChars);
    }

    private String resolveVerificationTemplateId() {
        String templateId = StrUtil.firstNonBlank(
                businessConfigService.getString(Constant.WX_MP_VERIFICATION_TEMPLATE_ID),
                WxConfigConstant.VERFICATION_CODE_TEMPLATE_ID
        );
        if (StrUtil.isBlank(templateId)) {
            throw new LinfengException("微信公众号验证码模板ID未配置");
        }
        return templateId;
    }

    private void ensureWechatConfigured() {
        WxMpConfigStorage configStorage = wxMpService.getWxMpConfigStorage();
        String appId = configStorage == null ? null : configStorage.getAppId();
        String secret = configStorage == null ? null : configStorage.getSecret();
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(secret)) {
            throw new LinfengException("微信公众号能力未配置完成，请先补齐 wechat_mp 三方配置");
        }
    }
}
