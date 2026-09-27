package org.aileme.shejiao.app.biz.wechat;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

/**
 * @desc 微信回复消息构建类
 */
public abstract class AbstractBuilder {

    /**
     * 构建xml消息内容
     *
     * @param content   消息内容
     * @param wxMessage 消息对象
     * @param service
     * @return
     */
    public abstract WxMpXmlOutMessage build(String content, WxMpXmlMessage wxMessage, WxMpService service);
}
