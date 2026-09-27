# Smart Companion Governance Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first governance foundation for AiLeMe's permissioned smart companion so message automation, gift execution, and content publishing can run behind explicit authorization, approvals, and audit trails.

**Architecture:** Keep Java as the trusted execution core and Python runtime as the planning boundary. Add app-side governance tables for permissions, approvals, tasks, and immutable logs; then expose those structures to later controller and service work without changing the existing agent endpoints yet.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, MySQL, FastAPI runtime, uni-app client

---

### Task 1: Add Governance Design And SQL Foundation

**Files:**
- Create: `docs/smart-companion-governance-design.md`
- Create: `agent_companion_governance_init.sql`
- Reference: `ai-le-me-agent-runtime/sql/001_agent_runtime_tables.sql`
- Reference: `gift_task_init.sql`

- [ ] **Step 1: Write the design doc**

```md
## 4. 核心原则

### 4.3 高风险动作必须经过审批门

- 大额礼物
- 首次代发消息
- 非白名单对象代发
- 夜间消息代发
- 线下约会确认
- 直接发布动态
```

- [ ] **Step 2: Add SQL for permission, task, approval, and log tables**

```sql
CREATE TABLE IF NOT EXISTS `agent_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL DEFAULT 0,
  `capability_code` varchar(64) NOT NULL DEFAULT '',
  `enabled` tinyint NOT NULL DEFAULT 0,
  `authorize_mode` varchar(32) NOT NULL DEFAULT 'manual_only',
  `target_scope` varchar(32) NOT NULL DEFAULT 'none',
  `risk_level` varchar(16) NOT NULL DEFAULT 'medium',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_permission_user_capability` (`user_id`, `capability_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: Verify the new SQL file is syntactically aligned with repo style**

Run: `sed -n '1,260p' /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/agent_companion_governance_init.sql`
Expected: four `CREATE TABLE IF NOT EXISTS` blocks with utf8mb4 comments and indexes

- [ ] **Step 4: Commit**

```bash
git add /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/docs/smart-companion-governance-design.md /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/agent_companion_governance_init.sql
git commit -m "docs: add smart companion governance foundation"
```

### Task 2: Add Java Domain Skeleton For Governance Tables

**Files:**
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentPermissionEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionTaskEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionApprovalEntity.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionLogEntity.java`
- Reference: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/GiftTaskEntity.java`

- [ ] **Step 1: Add the permission entity**

```java
@Data
@Entity
@Table(name = "agent_permission")
@TableName("agent_permission")
public class AgentPermissionEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;
    private String capabilityCode;
    private Integer enabled;
    private String authorizeMode;
    private String targetScope;
    private String riskLevel;
}
```

- [ ] **Step 2: Add the task, approval, and log entities with longtext fields mapped as `@Lob`**

```java
@Lob
private String planJson;

@Lob
private String payloadJson;
```

- [ ] **Step 3: Verify fields match SQL column names through camel-case mapping**

Run: `sed -n '1,220p' /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionTaskEntity.java`
Expected: `requestId`, `ownerUserId`, `targetUserId`, `runtimeTraceId`, `planJson`, `resultJson`

- [ ] **Step 4: Commit**

```bash
git add /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentPermissionEntity.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionTaskEntity.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionApprovalEntity.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/domain/entity/app/AgentActionLogEntity.java
git commit -m "feat: add agent governance entities"
```

### Task 3: Add Java DAO And Constant Layer

**Files:**
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentPermissionDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentActionTaskDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentActionApprovalDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentActionLogDao.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentGovernanceConstants.java`
- Reference: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/GiftTaskDao.java`

- [ ] **Step 1: Add MyBatis-Plus DAO interfaces**

```java
@Mapper
public interface AgentActionTaskDao extends BaseMapper<AgentActionTaskEntity> {
}
```

- [ ] **Step 2: Add shared capability and status constants**

```java
public static final String CAPABILITY_MESSAGE_AUTO_SEND = "message.auto.send";
public static final String CAPABILITY_GIFT_EXECUTE = "gift.execute";
public static final String TASK_STATUS_PENDING_APPROVAL = "pending_approval";
public static final String APPROVAL_STATUS_APPROVED = "approved";
```

- [ ] **Step 3: Verify the DAO package matches existing repo layout**

Run: `ls /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao | rg 'Agent'`
Expected: four new DAO files listed

- [ ] **Step 4: Commit**

```bash
git add /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentPermissionDao.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentActionTaskDao.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentActionApprovalDao.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/dao/AgentActionLogDao.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentGovernanceConstants.java
git commit -m "feat: add agent governance dao skeleton"
```

### Task 4: Wire Governance Into Existing Agent Entry Points

**Files:**
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppAgentController.java`
- Modify: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentOrchestratorService.java`
- Create: `ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentGovernanceService.java`
- Test: existing app smoke paths via manual API checks

- [ ] **Step 1: Add governance lookup before auto-execution paths**

```java
if (!agentGovernanceService.isCapabilityEnabled(me.getUid(), AgentGovernanceConstants.CAPABILITY_GIFT_EXECUTE)) {
    throw new LinfengException("当前未开启自动送礼权限");
}
```

- [ ] **Step 2: Create pending approval tasks when risk is high**

```java
if (agentGovernanceService.requiresApproval(me.getUid(), capabilityCode, riskLevel, targetUid, amount)) {
    return agentGovernanceService.createPendingApprovalTask(...);
}
```

- [ ] **Step 3: Manually verify gift and message flows still compile conceptually**

Run: `rg -n "executeGift|nextStep|reply\\(" /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent`
Expected: existing orchestration entry points still present and ready for governance hooks

- [ ] **Step 4: Commit**

```bash
git add /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/controller/AppAgentController.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentOrchestratorService.java /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix/app/service/agent/AgentGovernanceService.java
git commit -m "feat: wire agent governance into orchestrator"
```

## Self-Review

- Spec coverage: covers capability authorization, approval gates, action tasks, and audit logs; later controller/service wiring is explicitly staged in Task 4.
- Placeholder scan: no `TODO` or unspecified “add validation” placeholders remain.
- Type consistency: SQL names map to entity camel-case names and DAO names follow existing app package conventions.

Plan complete and saved to `docs/superpowers/plans/2026-04-03-smart-companion-governance-foundation.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
