package org.aileme.shejiao.app.biz.wechat;

import cn.hutool.core.util.StrUtil;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.WechatMpLoginSupportService;

import java.util.Map;
import java.util.Objects;

/**
 * @desc 扫码处理
 */
@Component
public class ScanHandler extends AbstractHandler {
    @Autowired
    private WechatMpLoginSupportService wechatMpLoginSupportService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> map,
                                    WxMpService wxMpService,
                                    WxSessionManager wxSessionManager) throws WxErrorException {
        // 扫码事件处理
        this.logger.info("扫码用户 OPENID: " + wxMessage.getFromUser());
        //todo 处理扫码相关的操作
        try {
            WxMpUser wxMpUser = wxMpService.getUserService().userInfo(wxMessage.getFromUser(), "zh_CN");
            if (Objects.isNull(wxMpUser)) {
                throw new Exception(StrUtil.format("获取用户信息为空,fromUserName={}", wxMessage.getFromUser()));
            }
            String token = wechatMpLoginSupportService.syncUserAndCacheLoginToken(wxMpUser, wxMessage.getEventKey());
            String reply = StrUtil.isNotBlank(token) ? "扫码成功，请返回爱了么完成登录" : "感谢关注爱了么";
            return new TextBuilder().build(reply, wxMessage, wxMpService);
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        }

        return null;
    }
}
