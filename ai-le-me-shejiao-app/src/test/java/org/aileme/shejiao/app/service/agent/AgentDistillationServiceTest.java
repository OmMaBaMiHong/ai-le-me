package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentDistillationServiceTest {

    @Test
    void shouldExposeAppFirstDistillationScenes() {
        AgentDistillationService service = new AgentDistillationService();

        List<AgentDistillationSceneVo> scenes = service.listScenes();

        assertEquals(2, scenes.size());
        assertEquals("self_bootstrap", scenes.get(0).getSceneType());
        assertEquals("private_person_analysis", scenes.get(1).getSceneType());
        assertFalse(scenes.get(1).getRelationPresets().isEmpty());
    }

    @Test
    void shouldMapRuntimeDistillationResponseToPreviewVo() {
        AgentRuntimeBridgeService bridgeService = mock(AgentRuntimeBridgeService.class);
        AgentDistillationSubjectDao subjectDao = mock(AgentDistillationSubjectDao.class);
        AgentDistillationSnapshotDao snapshotDao = mock(AgentDistillationSnapshotDao.class);
        AgentDistillationMaterialDao materialDao = mock(AgentDistillationMaterialDao.class);
        AgentDistillationService service = new AgentDistillationService();
        ReflectionTestUtils.setField(service, "agentRuntimeBridgeService", bridgeService);
        ReflectionTestUtils.setField(service, "agentDistillationSubjectDao", subjectDao);
        ReflectionTestUtils.setField(service, "agentDistillationSnapshotDao", snapshotDao);
        ReflectionTestUtils.setField(service, "agentDistillationMaterialDao", materialDao);

        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);
        AgentDistillationGenerateForm form = new AgentDistillationGenerateForm();
        form.setSceneType("private_person_analysis");
        form.setRelationLabel("前任");
        form.setSubjectName("A");

        JSONObject runtime = new JSONObject(true);
        runtime.put("scene_type", "private_person_analysis");
        runtime.put("subject_name", "A");
        runtime.put("relation_label", "前任");
        runtime.put("summary", "A 在关系压力上来时更容易先撤退。");
        runtime.put("core_insights", new JSONArray(List.of("更容易回避冲突")));
        runtime.put("interaction_guidance", new JSONArray(List.of("先低压沟通")));
        runtime.put("risk_flags", new JSONArray(List.of("不要做操控型建议")));
        runtime.put("confidence_notes", new JSONArray(List.of("截图内容暂未 OCR")));
        runtime.put("persona_kernel", new JSONObject(true));

        JSONArray evidenceCards = new JSONArray();
        evidenceCards.add(new JSONObject(true) {{
            put("kind", "material_signal");
            put("title", "关系材料已接入");
            put("detail", "已接入 2 份素材");
            put("source_types", new JSONArray(List.of("text_note", "chat_screenshot")));
        }});
        runtime.put("evidence_cards", evidenceCards);
        runtime.put("service_hooks", new JSONObject(true) {{
            put("recommended_opening_style", "先低压开场");
            put("matchmaker_style_hint", "慢热型");
            put("assistant_guardrails", new JSONArray(List.of("不要逼问")));
            put("profile_copy_hint", "不建议写进公开资料");
        }});

        when(bridgeService.generateDistillation(user, form)).thenReturn(runtime);
        when(subjectDao.selectOne(any())).thenReturn(null);
        when(subjectDao.insert(any(AgentDistillationSubjectEntity.class))).thenAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 11);
            return 1;
        });
        when(snapshotDao.insert(any(AgentDistillationSnapshotEntity.class))).thenAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 21);
            return 1;
        });

        AgentDistillationPreviewVo preview = service.generate(user, form);

        assertEquals("private_person_analysis", preview.getSceneType());
        assertEquals("A", preview.getSubjectName());
        assertEquals("A 在关系压力上来时更容易先撤退。", preview.getSummary());
        assertEquals(21, preview.getSnapshotId());
        assertFalse(preview.getEvidenceCards().isEmpty());
        assertEquals("material_signal", preview.getEvidenceCards().get(0).getKind());
        assertNotNull(preview.getServiceHooks());
        assertEquals("先低压开场", preview.getServiceHooks().getRecommendedOpeningStyle());
    }

    @Test
    void shouldPersistLatestSnapshotAndMaterialsWhenGenerate() {
        AgentRuntimeBridgeService bridgeService = mock(AgentRuntimeBridgeService.class);
        AgentDistillationSubjectDao subjectDao = mock(AgentDistillationSubjectDao.class);
        AgentDistillationSnapshotDao snapshotDao = mock(AgentDistillationSnapshotDao.class);
        AgentDistillationMaterialDao materialDao = mock(AgentDistillationMaterialDao.class);
        AgentDistillationService service = new AgentDistillationService();
        ReflectionTestUtils.setField(service, "agentRuntimeBridgeService", bridgeService);
        ReflectionTestUtils.setField(service, "agentDistillationSubjectDao", subjectDao);
        ReflectionTestUtils.setField(service, "agentDistillationSnapshotDao", snapshotDao);
        ReflectionTestUtils.setField(service, "agentDistillationMaterialDao", materialDao);

        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);

        AgentDistillationGenerateForm form = new AgentDistillationGenerateForm();
        form.setSceneType("self_bootstrap");
        form.setSubjectType("self");
        form.setRelationLabel("自己");
        form.setSubjectName("我自己");
        form.setAnalysisGoal("想更懂自己的关系模式");

        AgentDistillationGenerateForm.MaterialItem note = new AgentDistillationGenerateForm.MaterialItem();
        note.setMaterialType("text_note");
        note.setLabel("自述补充");
        note.setContent("我在关系里会先观察。");
        AgentDistillationGenerateForm.MaterialItem screenshot = new AgentDistillationGenerateForm.MaterialItem();
        screenshot.setMaterialType("chat_screenshot");
        screenshot.setLabel("截图1");
        screenshot.setFileUrl("https://cdn.test/1.png");
        form.setMaterials(List.of(note, screenshot));

        JSONObject runtime = new JSONObject(true);
        runtime.put("scene_type", "self_bootstrap");
        runtime.put("subject_name", "我自己");
        runtime.put("relation_label", "自己");
        runtime.put("summary", "你在关系里更偏慢热观察型。");
        runtime.put("core_insights", new JSONArray(List.of("先建立安全感再表达")));
        runtime.put("interaction_guidance", new JSONArray(List.of("适合低压表达")));
        runtime.put("risk_flags", new JSONArray(List.of("避免一上来过度迎合")));
        runtime.put("confidence_notes", new JSONArray(List.of("当前主要基于补充材料")));
        runtime.put("evidence_cards", new JSONArray());
        runtime.put("persona_kernel", new JSONObject(true));
        runtime.put("service_hooks", new JSONObject(true));
        when(bridgeService.generateDistillation(user, form)).thenReturn(runtime);
        when(subjectDao.selectOne(any())).thenReturn(null);
        when(subjectDao.insert(any(AgentDistillationSubjectEntity.class))).thenAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 11);
            return 1;
        });
        when(snapshotDao.insert(any(AgentDistillationSnapshotEntity.class))).thenAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 21);
            return 1;
        });

        AgentDistillationPreviewVo preview = service.generate(user, form);

        assertTrue(Boolean.TRUE.equals(preview.getSaved()));
        assertEquals(21, preview.getSnapshotId());
        assertEquals(2, preview.getMaterialCount());
        verify(subjectDao).insert(any(AgentDistillationSubjectEntity.class));
        verify(snapshotDao).insert(any(AgentDistillationSnapshotEntity.class));
        verify(subjectDao).updateById(any(AgentDistillationSubjectEntity.class));
        verify(materialDao, times(2)).insert(any(AgentDistillationMaterialEntity.class));
    }

    @Test
    void shouldLoadLatestSavedPreviewBySceneType() {
        AgentDistillationSnapshotDao snapshotDao = mock(AgentDistillationSnapshotDao.class);
        AgentDistillationService service = new AgentDistillationService();
        ReflectionTestUtils.setField(service, "agentDistillationSnapshotDao", snapshotDao);

        AgentDistillationSnapshotEntity snapshot = new AgentDistillationSnapshotEntity();
        snapshot.setId(21);
        snapshot.setUserId(1001);
        snapshot.setSceneType("self_bootstrap");
        snapshot.setSummary("你在关系里更偏慢热观察型。");
        snapshot.setMaterialCount(2);
        snapshot.setGeneratedAt(LocalDateTime.of(2026, 4, 8, 10, 30, 0));
        snapshot.setPreviewJson("{\"sceneType\":\"self_bootstrap\",\"subjectName\":\"我自己\",\"summary\":\"你在关系里更偏慢热观察型。\"}");
        when(snapshotDao.selectOne(any())).thenReturn(snapshot);

        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);

        AgentDistillationPreviewVo preview = service.getLatest(user, "self_bootstrap");

        assertNotNull(preview);
        assertEquals("self_bootstrap", preview.getSceneType());
        assertEquals("我自己", preview.getSubjectName());
        assertEquals(21, preview.getSnapshotId());
        assertEquals(2, preview.getMaterialCount());
        assertTrue(Boolean.TRUE.equals(preview.getSaved()));
    }
}
