# Relationship Distillation Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把现有 `Human 3.0` 风格的首登关系画像能力，和“蒸馏前任 / 蒸馏同事”的人物蒸馏方法，统一收束为 AiLeMe 的产品级“人物蒸馏平台”，先在 app 端可用，后续同一套 scene catalog 再接入广场宫格入口。

**Architecture:** 继续坚持 `Java 管产品与业务真源，Python runtime 管智能蒸馏与结构化输出`。一期不新起独立客户端，不新造第二套画像系统，而是在现有 `首登关系画像`、`编辑资料页`、`我的页`、`智能聊天助手` 之上，新增统一 `distillation` scene 合同、host 侧 subject/snapshot 持久化，以及 runtime 场景化蒸馏引擎。对外产品名统一为“人物蒸馏 / 关系理解”，内部 scene 先支持 `self_onboarding`、`ex_relationship`、`colleague_workstyle`。

**Tech Stack:** Java 21 + Spring Boot 3.5、MyBatis-Plus、FastAPI + Pydantic、uni-app Vue3、MySQL、现有 `AgentRuntimeBridgeService`、现有首登画像链路。

---

## File Map

### Runtime

- Create: `ai-le-me-agent-runtime/agent_runtime/schemas/distillation.py`
- Create: `ai-le-me-agent-runtime/agent_runtime/services/distillation.py`
- Create: `ai-le-me-agent-runtime/agent_runtime/api/routes/distillation.py`
- Create: `ai-le-me-agent-runtime/tests/test_distillation_api.py`
- Modify: `ai-le-me-agent-runtime/agent_runtime/api/deps.py`
- Modify: `ai-le-me-agent-runtime/agent_runtime/app.py`

### Java host

- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/AgentDistillationSubjectEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/AgentDistillationSnapshotEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentDistillationSubjectDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentDistillationSnapshotDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/param/app/AgentDistillationGenerateForm.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/param/app/AgentDistillationConfirmForm.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationSceneVo.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationSubjectVo.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationPreviewVo.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentDistillationService.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/test/java/td/matrix/app/service/agent/AgentDistillationServiceTest.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentRuntimeBridgeService.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppAgentController.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/resources/mapper/admin/*.xml` only if the repo convention requires XML mappings for the new entities

### App

- Create: `multi-platform-app/src/pages/user/edit-info/components/distillation-intake-flow.vue`
- Modify: `multi-platform-app/src/api/agent.js`
- Modify: `multi-platform-app/src/pages/tab/profile.vue`
- Modify: `multi-platform-app/src/pages/user/edit-info/edit.vue`
- Modify: `multi-platform-app/src/subpackages/chat/assistant.vue`

### SQL / Docs

- Create: `sql/20260408_agent_distillation.sql`
- Create: `docs/agent-distillation-product.md`
- Modify: `docs/user-onboarding-persona-v2.md`

## Product Decisions Locked In

- 对外产品不是“人格测试”也不是“前任模拟器”，统一叫“人物蒸馏 / 关系理解”。
- 一期 app 先做 `self_onboarding`、`ex_relationship`、`colleague_workstyle` 三个 scene。
- 一期不做微信/飞书全自动导入，先做 `手动问答 + 粘贴文本材料 + 已有画像复用`。
- `self_onboarding` 继续复用现有首登画像，不另造第二套 self persona。
- `ex_relationship` 输出重点是 `Relationship Memory + Persona + Closure/Pattern Insight`。
- `colleague_workstyle` 输出重点是 `Work Style + Collaboration Playbook + Risk/Boundary Insight`。
- 同一套 `scene catalog` 要同时服务：
  - 我的页的产品入口
  - 编辑资料页里的理解流
  - 聊天助手里的人物洞察入口
  - 后续广场宫格入口

### Task 1: 先把产品边界和 host 持久化骨架定住

**Files:**
- Create: `docs/agent-distillation-product.md`
- Create: `sql/20260408_agent_distillation.sql`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/AgentDistillationSubjectEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/AgentDistillationSnapshotEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentDistillationSubjectDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentDistillationSnapshotDao.java`

- [ ] **Step 1: 写清产品边界文档**

```markdown
# Agent Distillation Product

## Scenes
- self_onboarding
- ex_relationship
- colleague_workstyle

## Shared Output
- persona_kernel
- subject_summary
- core_insights
- recommended_approach
- avoid_signals
- tag_candidates

## Scene Extras
- ex_relationship: relationship_memory, closure_patterns
- colleague_workstyle: collaboration_playbook, pressure_triggers
```

- [ ] **Step 2: 先写 SQL 骨架**

```sql
CREATE TABLE IF NOT EXISTS `agent_distillation_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_user_id` int NOT NULL,
  `scene_type` varchar(32) NOT NULL,
  `subject_name` varchar(64) NOT NULL DEFAULT '',
  `relation_label` varchar(32) NOT NULL DEFAULT '',
  `status` tinyint NOT NULL DEFAULT 1,
  `latest_snapshot_id` bigint DEFAULT NULL,
  `latest_trace_id` varchar(64) NOT NULL DEFAULT '',
  `confirmed` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_scene` (`owner_user_id`, `scene_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物蒸馏主体';

CREATE TABLE IF NOT EXISTS `agent_distillation_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint NOT NULL,
  `scene_type` varchar(32) NOT NULL,
  `snapshot_json` longtext NOT NULL,
  `source_digest_json` longtext NULL,
  `trace_id` varchar(64) NOT NULL DEFAULT '',
  `version_no` int NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_subject_version` (`subject_id`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人物蒸馏结果快照';
```

- [ ] **Step 3: 建最小 Entity / Dao**

```java
@Data
@TableName("agent_distillation_subject")
public class AgentDistillationSubjectEntity implements Serializable {
    @TableId
    private Long id;
    private Integer ownerUserId;
    private String sceneType;
    private String subjectName;
    private String relationLabel;
    private Integer status;
    private Long latestSnapshotId;
    private String latestTraceId;
    private Integer confirmed;
    private Date createdAt;
    private Date updatedAt;
}
```

- [ ] **Step 4: 验证 SQL 与 Java 文件都落盘**

Run: `rg -n "agent_distillation_subject|agent_distillation_snapshot" /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/sql/20260408_agent_distillation.sql /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix`

Expected: 同时命中 SQL、Entity、Dao 文件。

- [ ] **Step 5: Commit**

```bash
git add docs/agent-distillation-product.md sql/20260408_agent_distillation.sql \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/AgentDistillationSubjectEntity.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/admin/AgentDistillationSnapshotEntity.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentDistillationSubjectDao.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentDistillationSnapshotDao.java
git commit -m "feat: add agent distillation host schema"
```

### Task 2: 在 runtime 里做统一 distillation scene 合同

**Files:**
- Create: `ai-le-me-agent-runtime/agent_runtime/schemas/distillation.py`
- Create: `ai-le-me-agent-runtime/agent_runtime/services/distillation.py`
- Create: `ai-le-me-agent-runtime/agent_runtime/api/routes/distillation.py`
- Create: `ai-le-me-agent-runtime/tests/test_distillation_api.py`
- Modify: `ai-le-me-agent-runtime/agent_runtime/api/deps.py`
- Modify: `ai-le-me-agent-runtime/agent_runtime/app.py`

- [ ] **Step 1: 先写 failing API test**

```python
from fastapi.testclient import TestClient
from agent_runtime.app import create_app

def test_distillation_generate_supports_three_scenes():
    client = TestClient(create_app())
    response = client.post("/distillation/profile/generate", json={
        "scene_type": "ex_relationship",
        "owner": {"user_id": 1001, "nickname": "我"},
        "subject": {"subject_name": "A", "relation_label": "前任"},
        "answers": [{"question_code": "breakup_pattern", "option_code": "silent_withdrawal"}],
        "materials": [{"kind": "text_note", "content": "吵架后会消失两天，再假装没事"}]
    })
    assert response.status_code == 200
    body = response.json()
    assert body["scene_type"] == "ex_relationship"
    assert body["subject_summary"]
    assert body["core_insights"]
```

- [ ] **Step 2: 运行测试确认当前缺失**

Run: `cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime && pytest tests/test_distillation_api.py -q`

Expected: FAIL，报 `/distillation/profile/generate` 不存在或 import 错误。

- [ ] **Step 3: 实现最小 schema / service / route**

```python
class DistillationGenerateRequest(BaseModel):
    scene_type: Literal["self_onboarding", "ex_relationship", "colleague_workstyle"]
    owner: UserCard
    subject: DistillationSubject
    answers: list[DistillationAnswer] = Field(default_factory=list)
    materials: list[DistillationMaterial] = Field(default_factory=list)

class DistillationGenerateResponse(BaseModel):
    scene_type: str
    subject_summary: str
    core_insights: list[str]
    recommended_approach: list[str]
    avoid_signals: list[str]
    tag_candidates: list[str]
    persona_kernel: dict[str, Any] = Field(default_factory=dict)
```

```python
@router.post("/profile/generate", response_model=DistillationGenerateResponse)
def generate_distillation(
    payload: DistillationGenerateRequest,
    service: DistillationService = Depends(get_distillation_service),
) -> DistillationGenerateResponse:
    return service.generate(payload)
```

- [ ] **Step 4: scene 规则先走 deterministic MVP**

```python
if payload.scene_type == "self_onboarding":
    return self._build_self_onboarding(payload)
if payload.scene_type == "ex_relationship":
    return self._build_ex_relationship(payload)
return self._build_colleague_workstyle(payload)
```

- [ ] **Step 5: 重新跑 runtime tests**

Run: `cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime && pytest tests/test_distillation_api.py tests/test_persona_api.py -q`

Expected: `2 passed` 或更多通过，且新接口返回三类 scene 的结构化结果。

- [ ] **Step 6: Commit**

```bash
git add ai-le-me-agent-runtime/agent_runtime/schemas/distillation.py \
  ai-le-me-agent-runtime/agent_runtime/services/distillation.py \
  ai-le-me-agent-runtime/agent_runtime/api/routes/distillation.py \
  ai-le-me-agent-runtime/agent_runtime/api/deps.py \
  ai-le-me-agent-runtime/agent_runtime/app.py \
  ai-le-me-agent-runtime/tests/test_distillation_api.py
git commit -m "feat: add runtime distillation scenes"
```

### Task 3: Java host 接 runtime，并把首登画像纳入统一 distillation 产品面

**Files:**
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/param/app/AgentDistillationGenerateForm.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/param/app/AgentDistillationConfirmForm.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationSceneVo.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationSubjectVo.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationPreviewVo.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentDistillationService.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/test/java/td/matrix/app/service/agent/AgentDistillationServiceTest.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentRuntimeBridgeService.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppAgentController.java`

- [ ] **Step 1: 先写 Java failing test**

```java
@Test
void generateShouldReuseOnboardingDraftForSelfScene() {
    AgentDistillationGenerateForm form = new AgentDistillationGenerateForm();
    form.setSceneType("self_onboarding");
    when(onboardingPersonaService.getMyDraft(1001)).thenReturn(buildDraft());

    AgentDistillationPreviewVo preview = service.generate(mockUser(1001), form);

    assertEquals("self_onboarding", preview.getSceneType());
    assertEquals("慢热认真型", preview.getArchetypeTitle());
    verify(agentRuntimeBridgeService, never()).generateDistillation(any());
}
```

- [ ] **Step 2: 运行 Maven 让它先红**

Run: `mvn -f /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/pom.xml -pl ai-le-me-modules/ai-le-me-shejiao-app -am -Dtest=AgentDistillationServiceTest test`

Expected: FAIL，缺类或缺方法。

- [ ] **Step 3: 实现 service 规则**

```java
if ("self_onboarding".equals(form.getSceneType())) {
    OnboardingPersonaDraftVO draft = onboardingPersonaService.getMyDraft(user.getUid());
    return AgentDistillationPreviewVo.fromOnboardingDraft(draft);
}
JSONObject runtimePreview = agentRuntimeBridgeService.generateDistillation(user, form);
AgentDistillationSubjectEntity subject = upsertSubject(user.getUid(), form, runtimePreview);
saveSnapshot(subject, runtimePreview);
return toPreviewVo(subject, runtimePreview);
```

- [ ] **Step 4: 给 AppAgentController 增加统一入口**

```java
@Login
@GetMapping("/distillation/scenes")
public Result<List<AgentDistillationSceneVo>> distillationScenes(@LoginUser AppUserEntity user) {
    return new Result<List<AgentDistillationSceneVo>>().ok(agentDistillationService.listScenes(user));
}

@Login
@PostMapping("/distillation/generate")
public Result<AgentDistillationPreviewVo> generateDistillation(@RequestBody AgentDistillationGenerateForm form,
                                                               @LoginUser AppUserEntity user) {
    return new Result<AgentDistillationPreviewVo>().ok(agentDistillationService.generate(user, form));
}
```

- [ ] **Step 5: 给 bridge 增加 runtime 请求映射**

```java
public JSONObject generateDistillation(AppUserEntity me, AgentDistillationGenerateForm form) {
    JSONObject payload = new JSONObject();
    payload.put("scene_type", form.getSceneType());
    payload.put("owner", buildUserCard(me));
    payload.put("subject", form.toRuntimeSubject());
    payload.put("answers", form.toRuntimeAnswers());
    payload.put("materials", form.toRuntimeMaterials());
    return postJson("/distillation/profile/generate", payload);
}
```

- [ ] **Step 6: 跑 Java 测试**

Run: `mvn -f /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/pom.xml -pl ai-le-me-modules/ai-le-me-shejiao-app -am -Dtest=AgentDistillationServiceTest,OnboardingPersonaServiceTest test`

Expected: PASS，且 `self_onboarding` 复用现有草稿，`ex/colleague` 走 runtime。

- [ ] **Step 7: Commit**

```bash
git add ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/param/app/AgentDistillationGenerateForm.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/param/app/AgentDistillationConfirmForm.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationSceneVo.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationSubjectVo.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/vo/AgentDistillationPreviewVo.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentDistillationService.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentRuntimeBridgeService.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppAgentController.java \
  ai-le-me-modules/ai-le-me-shejiao-app/src/test/java/td/matrix/app/service/agent/AgentDistillationServiceTest.java
git commit -m "feat: add app distillation service"
```

### Task 4: app 一期先并入“我的页 + 编辑资料页 + 聊天助手”，不新起独立主路由

**Files:**
- Create: `multi-platform-app/src/pages/user/edit-info/components/distillation-intake-flow.vue`
- Modify: `multi-platform-app/src/api/agent.js`
- Modify: `multi-platform-app/src/pages/tab/profile.vue`
- Modify: `multi-platform-app/src/pages/user/edit-info/edit.vue`
- Modify: `multi-platform-app/src/subpackages/chat/assistant.vue`

- [ ] **Step 1: 先补 app API**

```js
export function getDistillationScenes() {
  return http.get('agent/distillation/scenes');
}

export function generateDistillation(payload = {}) {
  return http.post('agent/distillation/generate', payload);
}
```

- [ ] **Step 2: profile 页展示统一入口**

```js
this.distillationScenes = (res?.result || []).filter(item => item && item.enabled !== false);
```

```vue
<view class="relationship-onboarding-card__actions">
  <view
    v-for="scene in distillationScenes"
    :key="scene.sceneType"
    class="relationship-onboarding-card__btn secondary"
    @click="openDistillationScene(scene)"
  >
    {{ scene.entryTitle }}
  </view>
</view>
```

- [ ] **Step 3: edit.vue 复用原有 route，按 query 切换 flow**

```js
const distillScene = options?.distillScene || '';
this.activeDistillScene = distillScene || 'self_onboarding';
this.showDistillationFlow = Boolean(options?.onboarding || distillScene);
```

```vue
<distillation-intake-flow
  v-if="showDistillationFlow"
  :scene-type="activeDistillScene"
  :draft="distillationDraft"
  @generated="handleDistillationGenerated"
  @confirmed="handleDistillationConfirmed"
/>
```

- [ ] **Step 4: 聊天助手挂“对象洞察”快捷入口**

```vue
<view class="assistant-panel__abilities">
  <view class="ability-chip" @tap="openTargetDistillation('ex_relationship')">
    <text class="ability-chip__title">关系复盘</text>
    <text class="ability-chip__state">人物蒸馏</text>
  </view>
</view>
```

- [ ] **Step 5: H5 build 验证**

Run: `cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app && npm run build:h5`

Expected: BUILD SUCCESS，无新的页面编译错误。

- [ ] **Step 6: 手工 smoke**

Run: `cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app && npm run dev:h5`

Expected:
- 我的页能看到“人物蒸馏”入口
- `self_onboarding` 仍复用现有首登画像
- `ex_relationship` / `colleague_workstyle` 能进入同一编辑资料页容器并拿到预览结果

- [ ] **Step 7: Commit**

```bash
git add multi-platform-app/src/api/agent.js \
  multi-platform-app/src/pages/tab/profile.vue \
  multi-platform-app/src/pages/user/edit-info/edit.vue \
  multi-platform-app/src/pages/user/edit-info/components/distillation-intake-flow.vue \
  multi-platform-app/src/subpackages/chat/assistant.vue
git commit -m "feat: add app distillation entry flows"
```

### Task 5: 把“首登关系画像”文档升级为平台文档，并补回归脚本

**Files:**
- Modify: `docs/user-onboarding-persona-v2.md`
- Modify: `docs/agent-distillation-product.md`
- Optional Create: `multi-platform-app/scripts/distillation-browser-check.js`

- [ ] **Step 1: 文档明确 self_onboarding 是平台 scene，不是独立烟囱**

```markdown
## 平台归属

`首登关系画像` 是 `人物蒸馏平台` 的 `self_onboarding` scene。
后续 `前任复盘` 与 `同事工作风格蒸馏` 复用同一 scene catalog、同一 host subject/snapshot、同一 runtime 输出合同。
```

- [ ] **Step 2: 补一个最小浏览器回归脚本**

```js
// 打开我的页 -> 点击人物蒸馏 -> 进入 self_onboarding -> 校验按钮存在
```

- [ ] **Step 3: 跑文档和脚本检查**

Run: `rg -n "self_onboarding|ex_relationship|colleague_workstyle" /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/docs /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app/scripts`

Expected: 三个 scene 都有文档或脚本落点。

- [ ] **Step 4: Commit**

```bash
git add docs/user-onboarding-persona-v2.md docs/agent-distillation-product.md multi-platform-app/scripts/distillation-browser-check.js
git commit -m "docs: position onboarding as distillation platform scene"
```

## Final Verification

- Runtime:
  - `cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime && pytest tests/test_distillation_api.py tests/test_persona_api.py tests/test_companion_autonomy_api.py -q`
- Java:
  - `mvn -f /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/pom.xml -pl ai-le-me-modules/ai-le-me-shejiao-app -am -Dtest=AgentDistillationServiceTest,OnboardingPersonaServiceTest,AgentAutonomyServiceTest test`
- App:
  - `cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/multi-platform-app && npm run build:h5`

Expected:
- runtime 新增 distillation scene 接口通过
- Java host 复用 self_onboarding，并能代理 ex/colleague scene
- app 无需新增主路由即可进入统一人物蒸馏 flow

## Rollout Notes

- `self_onboarding` 默认开启
- `ex_relationship`、`colleague_workstyle` 可先通过 Java scene catalog 标记 `enabled=true` 但 `entryPosition=profile_only`
- 广场宫格阶段只需要复用 `scene catalog` 返回值，不要再写第二套入口配置

## Spec Coverage Self-Review

- `Human 3.0`：
  - 已通过 `self_onboarding -> 人物蒸馏平台 scene` 收编，复用现有首登画像能力
- `蒸馏前任`：
  - 已在 `ex_relationship` scene 中落为结构化产品能力，不是单独 skill 仓库
- `蒸馏同事`：
  - 已在 `colleague_workstyle` scene 中落为结构化产品能力，不是单独 skill 仓库
- `先给 app 提供支持`：
  - 已明确复用 `我的页 + 编辑资料页 + 聊天助手`
- `后续放在广场宫格入口`：
  - 已通过 `scene catalog` 设计预留统一扩展位

Plan complete and saved to `docs/superpowers/plans/2026-04-08-relationship-distillation-platform.md`.

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints
