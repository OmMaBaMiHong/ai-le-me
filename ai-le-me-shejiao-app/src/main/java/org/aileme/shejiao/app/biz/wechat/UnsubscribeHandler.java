package org.aileme.shejiao.app.biz.wechat;

import java.util.Map;

import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

/**
 * @desc 订阅取消管理
 */
@Component
public class UnsubscribeHandler extends AbstractHandler {
    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
        Map<String, Object> context, WxMpService wxMpService,
        WxSessionManager sessionManager) {
        String openId = wxMessage.getFromUser();
        //根据openId查询unionId
        this.logger.info("取消关注用户 OPENID: " + openId);
        return null;
    }

}
