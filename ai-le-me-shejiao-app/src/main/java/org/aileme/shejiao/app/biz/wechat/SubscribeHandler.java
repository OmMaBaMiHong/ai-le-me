package org.aileme.shejiao.app.biz.wechat;

import cn.hutool.core.util.StrUtil;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.WechatMpLoginSupportService;

import java.util.Map;

/**
 * @desc 订阅处理
 */
@Component
public class SubscribeHandler extends AbstractHandler {
    @Autowired
    private WechatMpLoginSupportService wechatMpLoginSupportService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
        Map<String, Object> context, WxMpService weixinService,
        WxSessionManager sessionManager) throws WxErrorException {
        try {
            WxMpConfigStorage wxMpConfigStorage = weixinService.getWxMpConfigStorage();
            this.logger.info("当前公众号AppId:[{}]；新关注用户 OPENID: [{}]", wxMpConfigStorage.getAppId(),
                wxMessage.getFromUser());
            // TODO: 2023/3/27 此处可以循环调用KfSessionHandler发送消息
            // 事件KEY值，qrscene_为前缀，后面为二维码的参数值
            String eventKey = wxMessage.getEventKey();
            WxMpUser wxMpUser = weixinService.getUserService().userInfo(wxMessage.getFromUser(), "zh_CN");
            String token = wechatMpLoginSupportService.syncUserAndCacheLoginToken(wxMpUser, eventKey);
            String reply = StrUtil.isNotBlank(token) ? "关注成功，请返回爱了么完成登录" : "订阅成功";
            return new TextBuilder().build(reply, wxMessage, weixinService);
        } catch (
            Exception e) {
            this.logger.error(e.getMessage(), e);
        }
        return null;
    }
}
