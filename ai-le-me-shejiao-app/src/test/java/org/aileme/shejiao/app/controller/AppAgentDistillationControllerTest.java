package org.aileme.shejiao.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.aileme.shejiao.app.service.agent.AgentDistillationService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.AgentDistillationGenerateForm;
import org.aileme.shejiao.domain.vo.AgentDistillationPreviewVo;
import org.aileme.shejiao.domain.vo.AgentDistillationSceneVo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppAgentDistillationControllerTest {

    @Test
    void shouldReturnDistillationScenes() {
        AgentDistillationService service = mock(AgentDistillationService.class);
        AppAgentController controller = new AppAgentController();
        ReflectionTestUtils.setField(controller, "agentDistillationService", service);

        AgentDistillationSceneVo scene = new AgentDistillationSceneVo();
        scene.setSceneType("self_bootstrap");
        when(service.listScenes()).thenReturn(List.of(scene));

        Result<List<AgentDistillationSceneVo>> result = controller.distillationScenes();

        assertEquals(0, result.getCode());
        assertEquals(1, result.getResult().size());
        assertEquals("self_bootstrap", result.getResult().get(0).getSceneType());
    }

    @Test
    void shouldReturnDistillationPreview() {
        AgentDistillationService service = mock(AgentDistillationService.class);
        AppAgentController controller = new AppAgentController();
        ReflectionTestUtils.setField(controller, "agentDistillationService", service);

        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);
        AgentDistillationGenerateForm form = new AgentDistillationGenerateForm();
        AgentDistillationPreviewVo preview = new AgentDistillationPreviewVo();
        preview.setSceneType("private_person_analysis");
        preview.setSubjectName("A");
        when(service.generate(user, form)).thenReturn(preview);

        Result<AgentDistillationPreviewVo> result = controller.generateDistillation(form, user);

        assertEquals(0, result.getCode());
        assertSame(preview, result.getResult());
        assertEquals("A", result.getResult().getSubjectName());
    }

    @Test
    void shouldReturnLatestSavedDistillationPreview() {
        AgentDistillationService service = mock(AgentDistillationService.class);
        AppAgentController controller = new AppAgentController();
        ReflectionTestUtils.setField(controller, "agentDistillationService", service);

        AppUserEntity user = new AppUserEntity();
        user.setUid(1001);
        AgentDistillationPreviewVo preview = new AgentDistillationPreviewVo();
        preview.setSceneType("self_bootstrap");
        preview.setSubjectName("我自己");
        preview.setSnapshotId(21);
        when(service.getLatest(user, "self_bootstrap")).thenReturn(preview);

        Result<AgentDistillationPreviewVo> result = controller.latestDistillation("self_bootstrap", user);

        assertEquals(0, result.getCode());
        assertSame(preview, result.getResult());
        assertEquals(21, result.getResult().getSnapshotId());
    }
}
