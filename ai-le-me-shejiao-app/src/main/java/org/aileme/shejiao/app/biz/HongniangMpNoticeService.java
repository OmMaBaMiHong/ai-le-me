package org.aileme.shejiao.app.biz;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchRequestEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class HongniangMpNoticeService {

    private final WxMpService wxMpService;
    private final WechatRuntimeConfigService wechatRuntimeConfigService;

    public HongniangMpNoticeService(WxMpService wxMpService,
                                    WechatRuntimeConfigService wechatRuntimeConfigService) {
        this.wxMpService = wxMpService;
        this.wechatRuntimeConfigService = wechatRuntimeConfigService;
    }

    public void sendMatchRequestNotice(AppUserEntity targetUser,
                                       AppUserEntity requester,
                                       HongniangInfoEntity hongniang,
                                       HongniangMatchRequestEntity request) {
        if (targetUser == null || StringUtils.isBlank(targetUser.getMpOpenid())) {
            throw new LinfengException("目标用户未绑定公众号，无法发送牵线通知");
        }
        String templateId = StringUtils.trimToEmpty(wechatRuntimeConfigService.getMpMatchRequestTemplateId());
        if (StringUtils.isBlank(templateId)) {
            throw new LinfengException("微信公众号牵线通知模板未配置，请先补齐 wechat_mp.match_request_template_id");
        }
        String detailUrl = StringUtils.trimToEmpty(wechatRuntimeConfigService.getMpMatchRequestDetailUrl());
        if (StringUtils.isBlank(detailUrl)) {
            throw new LinfengException("微信公众号牵线详情跳转地址未配置，请先补齐 wechat_mp.match_request_detail_url");
        }

        WxMpTemplateMessage message = new WxMpTemplateMessage();
        message.setTemplateId(templateId);
        message.setToUser(targetUser.getMpOpenid());
        message.setUrl(buildDetailUrl(detailUrl, request));
        message.setData(buildTemplateData(requester, hongniang, request));
        try {
            wxMpService.getTemplateMsgService().sendTemplateMsg(message);
        } catch (Exception e) {
            throw new LinfengException("公众号牵线通知发送失败");
        }
    }

    private List<WxMpTemplateData> buildTemplateData(AppUserEntity requester,
                                                     HongniangInfoEntity hongniang,
                                                     HongniangMatchRequestEntity request) {
        List<WxMpTemplateData> data = new ArrayList<>();
        data.add(new WxMpTemplateData("keyword1", StringUtils.defaultIfBlank(requester == null ? null : requester.getUsername(), "新对象")));
        data.add(new WxMpTemplateData("keyword2", StringUtils.defaultIfBlank(hongniang == null ? null : hongniang.getHongniangName(), "红娘")));
        data.add(new WxMpTemplateData("keyword3", formatExpireTime(request == null ? null : request.getExpireTime())));
        data.add(new WxMpTemplateData("remark", buildRemark(request)));
        return data;
    }

    private String buildRemark(HongniangMatchRequestEntity request) {
        String message = request == null ? null : request.getRequestMessage();
        if (StringUtils.isNotBlank(message)) {
            return StringUtils.left(message.trim(), 120);
        }
        return "你收到一条新的牵线申请，点击查看详情并决定是否接受。";
    }

    private String buildDetailUrl(String baseUrl, HongniangMatchRequestEntity request) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl
            + separator + "requestId=" + safe(request == null ? null : request.getIntentRequestId())
            + "&caseId=" + safe(request == null || request.getCaseId() == null ? null : String.valueOf(request.getCaseId()))
            + "&requestChannel=" + safe(request == null || request.getRequestChannel() == null ? null : String.valueOf(request.getRequestChannel()));
    }

    private String formatExpireTime(java.util.Date expireTime) {
        if (expireTime == null) {
            return "请尽快处理";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(expireTime);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
