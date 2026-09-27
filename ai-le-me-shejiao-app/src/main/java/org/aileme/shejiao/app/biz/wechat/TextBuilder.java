package org.aileme.shejiao.app.biz.wechat;

import cn.hutool.core.util.RandomUtil;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;

/**
 * @desc 构建纯文本消息
 */
public class TextBuilder extends AbstractBuilder {

    @Override
    public WxMpXmlOutMessage build(String content, WxMpXmlMessage wxMessage,
                                   WxMpService service) {

        String resStr= null;
        if(content.startsWith("验证码")){
            resStr= RandomUtil.randomNumbers(6);
        }
        WxMpXmlOutTextMessage m = WxMpXmlOutMessage
                .TEXT()
                .content(resStr)
                .fromUser(wxMessage.getToUser())
                .toUser(wxMessage.getFromUser())
                .build();
        return m;
    }
}
