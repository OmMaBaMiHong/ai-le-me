package org.aileme.shejiao.app.config;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;
import org.aileme.shejiao.app.biz.wechat.*;

import static me.chanjar.weixin.common.api.WxConsts.EventType.SUBSCRIBE;
import static me.chanjar.weixin.common.api.WxConsts.EventType.UNSUBSCRIBE;
import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType.EVENT;
import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType.TEXT;
import static me.chanjar.weixin.mp.constant.WxMpEventConstants.CustomerService.*;
import static me.chanjar.weixin.mp.constant.WxMpEventConstants.POI_CHECK_NOTIFY;

/**
 * 微信公众号配置
 * 
 * @author system
 */
@Slf4j
@Configuration
public class WxConfig {

    private final WechatRuntimeConfigService wechatRuntimeConfigService;

    /**
     * 日志处理
     */
    private final LogHandler logHandler;
    private final NullHandler nullHandler;
    private final KfSessionHandler kfSessionHandler;
    private final StoreCheckNotifyHandler storeCheckNotifyHandler;
    private final LocationHandler locationHandler;
    private final MenuHandler menuHandler;
    private final MsgHandler msgHandler;
    private final UnsubscribeHandler unsubscribeHandler;
    private final SubscribeHandler subscribeHandler;
    private final ScanHandler scanHandler;
    private final TextMsgHandler textMsgHandler;
    private final ImgHandler imgHandler;

    public WxConfig(
        WechatRuntimeConfigService wechatRuntimeConfigService,
        LogHandler logHandler,
        NullHandler nullHandler,
        KfSessionHandler kfSessionHandler,
        StoreCheckNotifyHandler storeCheckNotifyHandler,
        LocationHandler locationHandler,
        MenuHandler menuHandler,
        MsgHandler msgHandler,
        UnsubscribeHandler unsubscribeHandler,
        SubscribeHandler subscribeHandler,
        ScanHandler scanHandler,
        TextMsgHandler textMsgHandler,
        ImgHandler imgHandler
    ) {
        this.wechatRuntimeConfigService = wechatRuntimeConfigService;
        this.logHandler = logHandler;
        this.nullHandler = nullHandler;
        this.kfSessionHandler = kfSessionHandler;
        this.storeCheckNotifyHandler = storeCheckNotifyHandler;
        this.locationHandler = locationHandler;
        this.menuHandler = menuHandler;
        this.msgHandler = msgHandler;
        this.unsubscribeHandler = unsubscribeHandler;
        this.subscribeHandler = subscribeHandler;
        this.scanHandler = scanHandler;
        this.textMsgHandler = textMsgHandler;
        this.imgHandler = imgHandler;
    }

    @Bean
    public WxMpService wxMpService() {
        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(wxMpConfigStorage());
        return wxMpService;
    }

    @Bean
    public WxMpConfigStorage wxMpConfigStorage() {
        WxMpDefaultConfigImpl wxMpConfigStorage = new WxMpDefaultConfigImpl();
        String resolvedAppId = wechatRuntimeConfigService.getMpAppId();
        String resolvedSecret = wechatRuntimeConfigService.getMpAppSecret();
        String resolvedToken = wechatRuntimeConfigService.getMpToken();
        if (!hasUsableConfig(resolvedAppId, resolvedSecret)) {
            log.warn("微信公众号三方配置未完成，当前以降级模式启动；补齐 wechat_mp 配置后相关能力自动恢复");
        }
        wxMpConfigStorage.setAppId(resolvedAppId);
        wxMpConfigStorage.setSecret(resolvedSecret);
        wxMpConfigStorage.setToken(resolvedToken);
        return wxMpConfigStorage;
    }

    @Bean
    public WxMpMessageRouter messageRouter(WxMpService wxMpService) {
        final WxMpMessageRouter newRouter = new WxMpMessageRouter(wxMpService);

        // 记录所有事件的日志 （异步执行）
        newRouter.rule().handler(this.logHandler).next();

        // 接收客服会话管理事件
        newRouter.rule().async(false).msgType(EVENT).event(KF_CREATE_SESSION)
                .handler(this.kfSessionHandler).end();
        newRouter.rule().async(false).msgType(EVENT).event(KF_CLOSE_SESSION)
                .handler(this.kfSessionHandler).end();
        newRouter.rule().async(false).msgType(EVENT).event(KF_SWITCH_SESSION)
                .handler(this.kfSessionHandler).end();

        // 门店审核事件
        newRouter.rule().async(false).msgType(EVENT).event(POI_CHECK_NOTIFY).handler(this.storeCheckNotifyHandler).end();

        // 自定义菜单事件
        newRouter.rule().async(false).msgType(EVENT).event(WxConsts.EventType.CLICK).handler(this.menuHandler).end();

        // 点击菜单连接事件
        newRouter.rule().async(false).msgType(EVENT).event(WxConsts.EventType.VIEW).handler(this.nullHandler).end();

        // 关注事件
        newRouter.rule().async(false).msgType(EVENT).event(SUBSCRIBE).handler(this.subscribeHandler).end();

        // 取消关注事件
        newRouter.rule().async(false).msgType(EVENT).event(UNSUBSCRIBE).handler(this.unsubscribeHandler).end();

        // 上报地理位置事件
        newRouter.rule().async(false).msgType(EVENT).event(WxConsts.EventType.LOCATION).handler(this.locationHandler).end();

        // 接收地理位置消息
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.LOCATION).handler(this.locationHandler).end();

        // 扫码事件
        newRouter.rule().async(false).msgType(EVENT).event(WxConsts.EventType.SCAN).handler(this.scanHandler).end();
        newRouter.rule().async(false).msgType(TEXT).handler(this.textMsgHandler).end();

        // 默认
        newRouter.rule().async(false).handler(this.msgHandler).end();

        return newRouter;
    }

    private boolean hasUsableConfig(String appId, String secret) {
        return StringUtils.isNotBlank(appId)
                && StringUtils.isNotBlank(secret);
    }
}
