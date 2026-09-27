package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.app.dao.AgentDistillationMaterialDao;
import org.aileme.shejiao.app.dao.AgentDistillationSnapshotDao;
import org.aileme.shejiao.app.dao.AgentDistillationSubjectDao;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.AgentDistillationMaterialEntity;
import org.aileme.shejiao.domain.entity.admin.AgentDistillationSnapshotEntity;
import org.aileme.shejiao.domain.entity.admin.AgentDistillationSubjectEntity;
import org.aileme.shejiao.domain.param.app.AgentDistillationGenerateForm;
import org.aileme.shejiao.domain.vo.AgentDistillationPreviewVo;
import org.aileme.shejiao.domain.vo.AgentDistillationSceneVo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AgentDistillationService {

    private static final String DEFAULT_SCENE = "self_bootstrap";
    private static final String DEFAULT_SELF_NAME = "我自己";
    private static final String DEFAULT_SELF_RELATION = "自己";
    private static final String DEFAULT_PRIVATE_NAME = "未命名对象";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    @Autowired
    private AgentDistillationSubjectDao agentDistillationSubjectDao;

    @Autowired
    private AgentDistillationSnapshotDao agentDistillationSnapshotDao;

    @Autowired
    private AgentDistillationMaterialDao agentDistillationMaterialDao;

    public List<AgentDistillationSceneVo> listScenes() {
        AgentDistillationSceneVo selfScene = new AgentDistillationSceneVo();
        selfScene.setSceneType("self_bootstrap");
        selfScene.setTitle("认识我");
        selfScene.setSubtitle("补充聊天记录、截图和自述，让画像更懂你。");
        selfScene.setDefaultRelationLabel("自己");
        selfScene.setRelationPresets(Collections.singletonList("自己"));
        selfScene.setMaterialHints(Arrays.asList("自述文本", "聊天截图", "生活截图"));

        AgentDistillationSceneVo privateScene = new AgentDistillationSceneVo();
        privateScene.setSceneType("private_person_analysis");
        privateScene.setTitle("识人分析");
        privateScene.setSubtitle("适合前任、同事等真实关系对象，用于复盘和沟通参考。");
        privateScene.setDefaultRelationLabel("前任");
        privateScene.setRelationPresets(Arrays.asList("前任", "同事"));
        privateScene.setMaterialHints(Arrays.asList("聊天记录", "截图", "补充描述"));

        return Arrays.asList(selfScene, privateScene);
    }

    public AgentDistillationPreviewVo generate(AppUserEntity user, AgentDistillationGenerateForm form) {
        AgentDistillationGenerateForm safeForm = form == null ? new AgentDistillationGenerateForm() : form;
        JSONObject runtime = agentRuntimeBridgeService.generateDistillation(user, safeForm);
        AgentDistillationPreviewVo preview = (runtime == null || runtime.isEmpty())
                ? buildFallbackPreview(safeForm)
                : toPreview(runtime, safeForm);
        persistLatestSnapshot(user, safeForm, preview);
        return preview;
    }

    public AgentDistillationPreviewVo getLatest(AppUserEntity user, String sceneType) {
        Integer userId = user == null ? null : user.getUid();
        if (userId == null) {
            return null;
        }
        AgentDistillationSnapshotEntity snapshot = agentDistillationSnapshotDao.selectOne(
                new LambdaQueryWrapper<AgentDistillationSnapshotEntity>()
                        .eq(AgentDistillationSnapshotEntity::getUserId, userId)
                        .eq(AgentDistillationSnapshotEntity::getSceneType, resolveSceneType(sceneType))
                        .eq(AgentDistillationSnapshotEntity::getStatus, 1)
                        .orderByDesc(AgentDistillationSnapshotEntity::getGeneratedAt)
                        .orderByDesc(AgentDistillationSnapshotEntity::getId)
                        .last("limit 1")
        );
        if (snapshot == null) {
            return null;
        }
        return restorePreview(snapshot);
    }

    private AgentDistillationPreviewVo buildFallbackPreview(AgentDistillationGenerateForm form) {
        AgentDistillationPreviewVo preview = new AgentDistillationPreviewVo();
        preview.setSceneType(resolveSceneType(form == null ? null : form.getSceneType()));
        preview.setSubjectName(resolveSubjectName(form));
        preview.setRelationLabel(resolveRelationLabel(form));
        preview.setSummary("当前已接入人物蒸馏入口，但运行时暂未返回结果，建议先补充更多文字材料后再次生成。");
        preview.setCoreInsights(Collections.singletonList("优先补充能体现关系节奏、冲突模式和边界表达的材料。"));
        preview.setInteractionGuidance(Collections.singletonList("先补充描述，再上传截图，会比只有图片更容易生成稳定结论。"));
        preview.setRiskFlags(Collections.singletonList("当前结果为降级兜底，不应作为最终判断。"));
        preview.setConfidenceNotes(Collections.singletonList("运行时暂不可用，当前为本地兜底结果。"));
        preview.setSaved(Boolean.FALSE);
        preview.setMaterialCount(countMaterials(form));
        AgentDistillationPreviewVo.ServiceHooks serviceHooks = new AgentDistillationPreviewVo.ServiceHooks();
        serviceHooks.setRecommendedOpeningStyle("先写清楚你最想弄明白的问题。");
        serviceHooks.setMatchmakerStyleHint("暂不建议下游消费。");
        serviceHooks.setAssistantGuardrails(Collections.singletonList("不要把兜底结果当成确定结论。"));
        serviceHooks.setProfileCopyHint("先补充材料，再决定是否写入资料。");
        preview.setServiceHooks(serviceHooks);
        return preview;
    }

    private AgentDistillationPreviewVo toPreview(JSONObject runtime, AgentDistillationGenerateForm form) {
        AgentDistillationPreviewVo preview = new AgentDistillationPreviewVo();
        preview.setSceneType(StringUtils.defaultIfBlank(runtime.getString("scene_type"), resolveSceneType(form == null ? null : form.getSceneType())));
        preview.setSubjectName(StringUtils.defaultIfBlank(runtime.getString("subject_name"), resolveSubjectName(form)));
        preview.setRelationLabel(StringUtils.defaultIfBlank(runtime.getString("relation_label"), resolveRelationLabel(form)));
        preview.setSummary(StringUtils.defaultIfBlank(runtime.getString("summary"), ""));
        preview.setCoreInsights(toStringList(runtime.getJSONArray("core_insights")));
        preview.setInteractionGuidance(toStringList(runtime.getJSONArray("interaction_guidance")));
        preview.setRiskFlags(toStringList(runtime.getJSONArray("risk_flags")));
        preview.setConfidenceNotes(toStringList(runtime.getJSONArray("confidence_notes")));
        preview.setEvidenceCards(toEvidenceCards(runtime.getJSONArray("evidence_cards")));
        preview.setPersonaKernel(toObjectMap(runtime.getJSONObject("persona_kernel")));
        preview.setServiceHooks(toServiceHooks(runtime.getJSONObject("service_hooks")));
        preview.setSaved(Boolean.FALSE);
        preview.setMaterialCount(countMaterials(form));
        return preview;
    }

    private void persistLatestSnapshot(AppUserEntity user,
                                       AgentDistillationGenerateForm form,
                                       AgentDistillationPreviewVo preview) {
        Integer userId = user == null ? null : user.getUid();
        if (userId == null || preview == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String sceneType = resolveSceneType(form == null ? null : form.getSceneType());
        String subjectName = resolveSubjectName(form);
        AgentDistillationSubjectEntity subject = agentDistillationSubjectDao.selectOne(
                new LambdaQueryWrapper<AgentDistillationSubjectEntity>()
                        .eq(AgentDistillationSubjectEntity::getUserId, userId)
                        .eq(AgentDistillationSubjectEntity::getSceneType, sceneType)
                        .eq(AgentDistillationSubjectEntity::getSubjectName, subjectName)
                        .eq(AgentDistillationSubjectEntity::getStatus, 1)
                        .orderByDesc(AgentDistillationSubjectEntity::getId)
                        .last("limit 1")
        );
        if (subject == null) {
            subject = new AgentDistillationSubjectEntity();
            subject.setUserId(userId);
            subject.setSceneType(sceneType);
            subject.setSubjectType(resolveSubjectType(form));
            subject.setSubjectName(subjectName);
            subject.setRelationLabel(resolveRelationLabel(form));
            subject.setStatus(1);
            subject.setCreateTime(now);
            subject.setUpdateTime(now);
            agentDistillationSubjectDao.insert(subject);
        } else {
            subject.setSubjectType(resolveSubjectType(form));
            subject.setRelationLabel(resolveRelationLabel(form));
            subject.setStatus(1);
            subject.setUpdateTime(now);
            agentDistillationSubjectDao.updateById(subject);
        }

        AgentDistillationSnapshotEntity snapshot = new AgentDistillationSnapshotEntity();
        snapshot.setUserId(userId);
        snapshot.setSubjectId(subject.getId());
        snapshot.setSceneType(sceneType);
        snapshot.setSummary(StringUtils.defaultString(preview.getSummary()));
        snapshot.setAnalysisGoal(normalizeText(form == null ? null : form.getAnalysisGoal()));
        snapshot.setPreviewJson(JSON.toJSONString(preview));
        snapshot.setMaterialCount(countMaterials(form));
        snapshot.setSource("app_distillation");
        snapshot.setStatus(1);
        snapshot.setGeneratedAt(now);
        snapshot.setCreateTime(now);
        snapshot.setUpdateTime(now);
        agentDistillationSnapshotDao.insert(snapshot);

        subject.setLastSnapshotId(snapshot.getId());
        subject.setUpdateTime(now);
        agentDistillationSubjectDao.updateById(subject);

        persistMaterials(subject.getId(), snapshot.getId(), form == null ? null : form.getMaterials(), now);
        fillSnapshotMeta(preview, snapshot);
    }

    private void persistMaterials(Integer subjectId,
                                  Integer snapshotId,
                                  List<AgentDistillationGenerateForm.MaterialItem> materials,
                                  LocalDateTime now) {
        if (subjectId == null || snapshotId == null || materials == null || materials.isEmpty()) {
            return;
        }
        for (int i = 0; i < materials.size(); i++) {
            AgentDistillationGenerateForm.MaterialItem item = materials.get(i);
            if (item == null) {
                continue;
            }
            AgentDistillationMaterialEntity entity = new AgentDistillationMaterialEntity();
            entity.setSubjectId(subjectId);
            entity.setSnapshotId(snapshotId);
            entity.setMaterialType(normalizeText(item.getMaterialType()));
            entity.setLabel(normalizeText(item.getLabel()));
            entity.setContent(normalizeText(item.getContent()));
            entity.setFileUrl(normalizeText(item.getFileUrl()));
            entity.setSortNo(i + 1);
            entity.setStatus(1);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            agentDistillationMaterialDao.insert(entity);
        }
    }

    private AgentDistillationPreviewVo restorePreview(AgentDistillationSnapshotEntity snapshot) {
        AgentDistillationPreviewVo preview = null;
        if (StringUtils.isNotBlank(snapshot.getPreviewJson())) {
            try {
                preview = JSON.parseObject(snapshot.getPreviewJson(), AgentDistillationPreviewVo.class);
            } catch (Exception ignored) {
            }
        }
        if (preview == null) {
            preview = new AgentDistillationPreviewVo();
        }
        preview.setSceneType(StringUtils.defaultIfBlank(preview.getSceneType(), snapshot.getSceneType()));
        preview.setSubjectName(StringUtils.defaultIfBlank(preview.getSubjectName(), resolveDefaultSubjectName(snapshot.getSceneType())));
        preview.setSummary(StringUtils.defaultIfBlank(preview.getSummary(), snapshot.getSummary()));
        fillSnapshotMeta(preview, snapshot);
        return preview;
    }

    private void fillSnapshotMeta(AgentDistillationPreviewVo preview, AgentDistillationSnapshotEntity snapshot) {
        if (preview == null || snapshot == null) {
            return;
        }
        preview.setSnapshotId(snapshot.getId());
        preview.setSaved(Boolean.TRUE);
        preview.setMaterialCount(snapshot.getMaterialCount());
        preview.setGeneratedAt(formatDateTime(snapshot.getGeneratedAt()));
    }

    private int countMaterials(AgentDistillationGenerateForm form) {
        return form == null || form.getMaterials() == null ? 0 : form.getMaterials().size();
    }

    private String resolveSceneType(String sceneType) {
        return StringUtils.defaultIfBlank(sceneType, DEFAULT_SCENE);
    }

    private String resolveSubjectType(AgentDistillationGenerateForm form) {
        if (form == null || StringUtils.isBlank(form.getSubjectType())) {
            return Objects.equals(resolveSceneType(form == null ? null : form.getSceneType()), DEFAULT_SCENE) ? "self" : "private_person";
        }
        return form.getSubjectType();
    }

    private String resolveSubjectName(AgentDistillationGenerateForm form) {
        String fallback = resolveDefaultSubjectName(resolveSceneType(form == null ? null : form.getSceneType()));
        return StringUtils.defaultIfBlank(form == null ? null : form.getSubjectName(), fallback);
    }

    private String resolveRelationLabel(AgentDistillationGenerateForm form) {
        String sceneType = resolveSceneType(form == null ? null : form.getSceneType());
        String fallback = Objects.equals(sceneType, DEFAULT_SCENE) ? DEFAULT_SELF_RELATION : "前任";
        return StringUtils.defaultIfBlank(form == null ? null : form.getRelationLabel(), fallback);
    }

    private String resolveDefaultSubjectName(String sceneType) {
        return Objects.equals(resolveSceneType(sceneType), DEFAULT_SCENE) ? DEFAULT_SELF_NAME : DEFAULT_PRIVATE_NAME;
    }

    private String normalizeText(String value) {
        return StringUtils.trimToNull(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATETIME_FORMATTER);
    }

    private List<AgentDistillationPreviewVo.EvidenceCard> toEvidenceCards(JSONArray source) {
        List<AgentDistillationPreviewVo.EvidenceCard> items = new ArrayList<>();
        if (source == null) {
            return items;
        }
        for (int i = 0; i < source.size(); i++) {
            JSONObject item = source.getJSONObject(i);
            if (item == null) {
                continue;
            }
            AgentDistillationPreviewVo.EvidenceCard card = new AgentDistillationPreviewVo.EvidenceCard();
            card.setKind(item.getString("kind"));
            card.setTitle(item.getString("title"));
            card.setDetail(item.getString("detail"));
            card.setSourceTypes(toStringList(item.getJSONArray("source_types")));
            items.add(card);
        }
        return items;
    }

    private AgentDistillationPreviewVo.ServiceHooks toServiceHooks(JSONObject source) {
        AgentDistillationPreviewVo.ServiceHooks hooks = new AgentDistillationPreviewVo.ServiceHooks();
        if (source == null) {
            return hooks;
        }
        hooks.setRecommendedOpeningStyle(source.getString("recommended_opening_style"));
        hooks.setMatchmakerStyleHint(source.getString("matchmaker_style_hint"));
        hooks.setAssistantGuardrails(toStringList(source.getJSONArray("assistant_guardrails")));
        hooks.setProfileCopyHint(source.getString("profile_copy_hint"));
        return hooks;
    }

    private Map<String, Object> toObjectMap(JSONObject source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(source);
    }

    private List<String> toStringList(JSONArray source) {
        List<String> items = new ArrayList<>();
        if (source == null) {
            return items;
        }
        for (int i = 0; i < source.size(); i++) {
            String value = source.getString(i);
            if (StringUtils.isNotBlank(value)) {
                items.add(value);
            }
        }
        return items;
    }
}
