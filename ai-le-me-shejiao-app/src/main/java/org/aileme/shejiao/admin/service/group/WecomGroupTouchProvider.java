package org.aileme.shejiao.admin.service.group;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WecomGroupTouchProvider implements GroupTouchProvider {

    private static final String SERVICE_TYPE = "wecom_customer";

    private final ISysThirdPartyService thirdPartyService;
    private final WecomCustomerApiClient wecomCustomerApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WecomGroupTouchProvider(ISysThirdPartyService thirdPartyService,
                                   WecomCustomerApiClient wecomCustomerApiClient) {
        this.thirdPartyService = thirdPartyService;
        this.wecomCustomerApiClient = wecomCustomerApiClient;
    }

    @Override
    public int providerType() {
        return 1;
    }

    @Override
    public ProviderResult syncGroup(HongniangWechatGroupEntity group) {
        String corpId = thirdPartyService.getConfigValue(SERVICE_TYPE, "corpId");
        String corpSecret = thirdPartyService.getConfigValue(SERVICE_TYPE, "corpSecret");
        if (StringUtils.isAnyBlank(corpId, corpSecret)) {
            return ProviderResult.builder()
                .success(false)
                .summary("企业微信配置缺失，请先补齐 wecom_customer 配置")
                .rawResponse("{\"message\":\"missing wecom_customer configs\"}")
                .build();
        }
        if (StringUtils.isBlank(group.getExternalGroupId())) {
            return ProviderResult.builder()
                .success(false)
                .summary("企微客户群缺少 externalGroupId，无法同步")
                .rawResponse("{\"message\":\"missing external_group_id\"}")
                .build();
        }
        try {
            String accessToken = wecomCustomerApiClient.fetchAccessToken(corpId, corpSecret);
            Map<String, Object> response = wecomCustomerApiClient.getGroupChat(accessToken, group.getExternalGroupId());
            Map<String, Object> groupChat = asMap(response.get("group_chat"));
            String groupName = text(groupChat.get("name"));
            String ownerUserId = text(groupChat.get("owner"));
            int memberCount = asList(groupChat.get("member_list")).size();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("groupName", groupName);
            data.put("ownerWechat", ownerUserId);
            data.put("memberCount", memberCount);
            return ProviderResult.builder()
                .success(true)
                .summary(String.format("已同步企微客户群：%s（%d人）",
                    StringUtils.defaultIfBlank(groupName, StringUtils.defaultIfBlank(group.getGroupName(), "未命名群")),
                    memberCount))
                .rawResponse(toJson(response))
                .data(data)
                .build();
        } catch (Exception e) {
            return ProviderResult.builder()
                .success(false)
                .summary(StringUtils.defaultIfBlank(e.getMessage(), "企业微信同步失败"))
                .rawResponse("{\"message\":\"wecom sync failed\"}")
                .build();
        }
    }

    @Override
    public ProviderResult submitTask(HongniangWechatGroupEntity group, HongniangGroupTouchTaskEntity task) {
        String corpId = thirdPartyService.getConfigValue(SERVICE_TYPE, "corpId");
        String corpSecret = thirdPartyService.getConfigValue(SERVICE_TYPE, "corpSecret");
        if (StringUtils.isAnyBlank(corpId, corpSecret)) {
            return ProviderResult.builder()
                .success(false)
                .summary("企业微信配置缺失，请先补齐 corpId/corpSecret")
                .rawResponse("{\"message\":\"missing wecom_customer task configs\"}")
                .build();
        }
        if (StringUtils.isBlank(group.getExternalGroupId())) {
            return ProviderResult.builder()
                .success(false)
                .summary("企微客户群缺少 externalGroupId，无法下发群发任务")
                .rawResponse("{\"message\":\"missing external_group_id\"}")
                .build();
        }
        String sender = resolveSender(group, task);
        if (StringUtils.isBlank(sender)) {
            return ProviderResult.builder()
                .success(false)
                .summary("企业微信群发缺少发送成员，请先同步群信息或填写群主企微成员ID")
                .rawResponse("{\"message\":\"missing sender\"}")
                .build();
        }
        try {
            String accessToken = wecomCustomerApiClient.fetchAccessToken(corpId, corpSecret);
            Map<String, Object> payload = buildTaskPayload(group, task, sender);
            Map<String, Object> response = wecomCustomerApiClient.addGroupMassMessage(accessToken, payload);
            String msgId = text(response.get("msgid"));
            return ProviderResult.builder()
                .success(true)
                .providerTaskId(StringUtils.defaultIfBlank(msgId, "WECOM-" + System.currentTimeMillis()))
                .summary("企业微信群发任务创建成功")
                .rawResponse(toJson(response))
                .data(Map.of("sender", sender))
                .build();
        } catch (Exception e) {
            return ProviderResult.builder()
                .success(false)
                .summary(StringUtils.defaultIfBlank(e.getMessage(), "企业微信群发任务创建失败"))
                .rawResponse("{\"message\":\"wecom task failed\"}")
                .build();
        }
    }

    private String resolveSender(HongniangWechatGroupEntity group, HongniangGroupTouchTaskEntity task) {
        Map<String, Object> payload = parsePayload(task.getContentPayload());
        String sender = text(payload.get("sender"));
        if (StringUtils.isNotBlank(sender)) {
            return sender;
        }
        return StringUtils.trimToEmpty(group.getOwnerWechat());
    }

    private Map<String, Object> buildTaskPayload(HongniangWechatGroupEntity group,
                                                 HongniangGroupTouchTaskEntity task,
                                                 String sender) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_type", "group");
        payload.put("chat_id_list", List.of(group.getExternalGroupId()));
        payload.put("sender", sender);
        String textContent = buildTextContent(task);
        List<Map<String, Object>> attachments = buildAttachments(task);
        if (StringUtils.isNotBlank(textContent)) {
            payload.put("text", Map.of("content", textContent));
        }
        if (!attachments.isEmpty()) {
            payload.put("attachments", attachments);
        }
        if (!payload.containsKey("text") && attachments.isEmpty()) {
            throw new IllegalArgumentException("群发内容不能为空");
        }
        return payload;
    }

    private String buildTextContent(HongniangGroupTouchTaskEntity task) {
        if (task == null) {
            return "";
        }
        Map<String, Object> payload = parsePayload(task.getContentPayload());
        if (task.getContentType() != null && task.getContentType() == 1) {
            String jsonContent = text(payload.get("content"));
            if (StringUtils.isNotBlank(jsonContent)) {
                return jsonContent;
            }
            Map<String, Object> textNode = asMap(payload.get("text"));
            jsonContent = text(textNode.get("content"));
            if (StringUtils.isNotBlank(jsonContent)) {
                return jsonContent;
            }
            return StringUtils.trimToEmpty(task.getContentPayload());
        }
        return text(payload.get("content"));
    }

    private List<Map<String, Object>> buildAttachments(HongniangGroupTouchTaskEntity task) {
        if (task == null || task.getContentType() == null || task.getContentType() == 1) {
            return List.of();
        }
        Map<String, Object> payload = parsePayload(task.getContentPayload());
        String url = text(payload.get("url"));
        String picUrl = text(payload.get("picUrl"));
        if (StringUtils.isBlank(picUrl)) {
            picUrl = text(payload.get("picurl"));
        }
        if (StringUtils.isBlank(url) && task.getContentType() == 3 && looksLikeUrl(task.getContentPayload())) {
            url = StringUtils.trim(task.getContentPayload());
        }
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("链接/图文消息必须提供 url");
        }
        String title = StringUtils.defaultIfBlank(text(payload.get("title")), StringUtils.defaultIfBlank(task.getTaskName(), "群发消息"));
        String desc = StringUtils.defaultIfBlank(text(payload.get("desc")), text(payload.get("content")));
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("msgtype", "link");
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("title", title);
        link.put("url", url);
        if (StringUtils.isNotBlank(desc)) {
            link.put("desc", desc);
        }
        if (StringUtils.isNotBlank(picUrl)) {
            link.put("picurl", picUrl);
        }
        attachment.put("link", link);
        List<Map<String, Object>> attachments = new ArrayList<>();
        attachments.add(attachment);
        return attachments;
    }

    private Map<String, Object> parsePayload(String payload) {
        if (StringUtils.isBlank(payload)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean looksLikeUrl(String value) {
        String normalized = StringUtils.trimToEmpty(value);
        return StringUtils.startsWithIgnoreCase(normalized, "http://")
            || StringUtils.startsWithIgnoreCase(normalized, "https://");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
