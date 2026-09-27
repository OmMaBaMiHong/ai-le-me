from __future__ import annotations

from pathlib import Path

from agent_runtime.core.config import Settings
from agent_runtime.integrations.base import IntegrationProfile
from agent_runtime.integrations.graph import GraphCatalog
from agent_runtime.integrations.memory import MemoryCatalog
from agent_runtime.integrations.workflow import WorkflowCatalog
from agent_runtime.schemas.runtime import (
    CoreProductDescriptor,
    EvalMetricDescriptor,
    IntegrationDescriptor,
    PolicyCheckDescriptor,
    PolicySurfaceDescriptor,
    RuntimeArchitectureSnapshot,
    RuntimeLayerDescriptor,
    RuntimeModuleDescriptor,
    RuntimePlaneDescriptor,
)
from agent_runtime.services.evals import AgentEvalCatalog
from agent_runtime.services.policy import AgentPolicyCatalog


class RuntimeArchitectureService:
    def __init__(
        self,
        settings: Settings,
        workflow_catalog: WorkflowCatalog,
        memory_catalog: MemoryCatalog,
        graph_catalog: GraphCatalog,
        policy_catalog: AgentPolicyCatalog,
        eval_catalog: AgentEvalCatalog,
    ) -> None:
        self.settings = settings
        self.workflow_catalog = workflow_catalog
        self.memory_catalog = memory_catalog
        self.graph_catalog = graph_catalog
        self.policy_catalog = policy_catalog
        self.eval_catalog = eval_catalog

    def snapshot(self) -> RuntimeArchitectureSnapshot:
        docs_path = Path(__file__).resolve().parents[2] / "docs" / "agent-product-architecture.md"
        return RuntimeArchitectureSnapshot(
            strategy_name="java-core-python-runtime-hybrid",
            north_star=(
                "Build a Claude-Code-style relationship agent runtime that can observe, recall, reason, "
                "plan, act under policy, and continuously improve across host applications."
            ),
            positioning=(
                "A standalone relationship-agent runtime product that operates inside AiLeMe today "
                "and remains extraction-ready for future multi-tenant commercialization."
            ),
            java_core="Java remains the source of truth for business rules, permissions, execution, and audit.",
            workflow=self._to_descriptor(self.workflow_catalog.resolve()),
            memory=[self._to_descriptor(item) for item in self.memory_catalog.list_profiles()],
            graph=[self._to_descriptor(item) for item in self.graph_catalog.list_profiles()],
            skills=IntegrationDescriptor(
                code=self.settings.skills_reference,
                name="OpenClaw-inspired Skills",
                role="Controlled adapters for skills, voice, and channel expansion without arbitrary execution.",
                selected=True,
                enabled=self.settings.skills_adapter_mode == "controlled_adapter",
                installed=False,
                docs_url="https://docs.openclaw.ai/tools/skills",
                notes=[
                    "Use as product inspiration only, not as the trusted execution core.",
                    "Keep Java approval and safety gates in front of any externalized action.",
                ],
            ),
            runtime_spine=[
                RuntimeLayerDescriptor(
                    code="observe",
                    name="Observe",
                    responsibility="Normalize conversation, profile, event, content, and recommendation signals.",
                    current_state="Implemented as request payload assembly and contextual signal collection.",
                    next_builds=["photo and moment signal ingestion", "event-driven observation hooks"],
                ),
                RuntimeLayerDescriptor(
                    code="recall",
                    name="Recall",
                    responsibility="Unify lexical, vector, graph, and historical memory retrieval.",
                    current_state="Implemented with vector recall, lexical hints, graph facts, and runtime traces.",
                    next_builds=["memory condensation jobs", "pair-level recall layering", "reranker support"],
                ),
                RuntimeLayerDescriptor(
                    code="reason",
                    name="Reason",
                    responsibility="Infer stage, preference, risk, and relationship opportunity from evidence.",
                    current_state="Implemented through structured persona, matchmaker, and companion generation.",
                    next_builds=["evidence-weighted scoring", "style routing", "multi-pass reasoning"],
                ),
                RuntimeLayerDescriptor(
                    code="plan",
                    name="Plan",
                    responsibility="Convert reasoning into next-step strategies, task sequences, and standing orders.",
                    current_state="Implemented as next-step and recommendation outputs, not yet as durable workflow runs.",
                    next_builds=["task graph persistence", "scheduled progression loops", "subtask decomposition"],
                ),
                RuntimeLayerDescriptor(
                    code="policy",
                    name="Policy",
                    responsibility="Gate actions using entitlement, authorization, risk, pacing, and budget rules.",
                    current_state="Partially implemented in host-side Java policy and billing flows; Python side now catalogs the required policy surfaces.",
                    next_builds=["runtime policy engine", "preflight policy decisions", "host-policy contract"],
                ),
                RuntimeLayerDescriptor(
                    code="act",
                    name="Act",
                    responsibility="Emit typed actions for messages, gifts, moments, dating, and outreach.",
                    current_state="Implemented as structured suggested actions rather than a general tool execution kernel.",
                    next_builds=["typed tool registry", "tool hooks", "action execution adapters"],
                ),
                RuntimeLayerDescriptor(
                    code="reflect",
                    name="Reflect",
                    responsibility="Write outcomes back into memory, stage state, and future strategy.",
                    current_state="Implemented through trace persistence and memory recording for persona and companion flows.",
                    next_builds=["action outcome ingestion", "failure learning", "memory summarization"],
                ),
                RuntimeLayerDescriptor(
                    code="eval",
                    name="Eval",
                    responsibility="Measure quality, safety, conversion, and cost to improve the runtime as a product.",
                    current_state="Not yet a first-class center; trace data exists but scorecards are not formalized.",
                    next_builds=["offline benchmarks", "live KPI scorecards", "prompt and route experiments"],
                ),
            ],
            product_planes=[
                RuntimePlaneDescriptor(
                    code="identity_tenant_channel",
                    name="Identity / Tenant / Channel",
                    responsibility="Provide tenant isolation, agent identity, channel adapters, and host integration boundaries.",
                    modules=["channel_gateway", "agent_identity", "workspace_isolation"],
                    notes=["Required before the runtime can become a generalized external product."],
                ),
                RuntimePlaneDescriptor(
                    code="governance_economics",
                    name="Governance / Safety / Economics",
                    responsibility="Provide capability switches, policy, provider budget governance, and billing compatibility.",
                    modules=["policy_engine", "entitlement_adapter", "budget_governor"],
                    notes=["This plane keeps high-autonomy features commercially safe."],
                ),
                RuntimePlaneDescriptor(
                    code="operations_improvement",
                    name="Operations / Observability / Improvement",
                    responsibility="Provide traces, replay, evals, experiments, and production optimization loops.",
                    modules=["trace_center", "eval_center", "experiment_control"],
                    notes=["This plane converts agent behavior into measurable operating leverage."],
                ),
            ],
            platform_modules=[
                RuntimeModuleDescriptor(
                    code="agent_kernel",
                    name="Agent Kernel",
                    role="Owns task loops, hook points, and future subagent-ready orchestration.",
                    owner_boundary="Python runtime owned",
                    maturity="planned",
                    notes=["Inspired by Claude Code orchestration patterns, but specialized for relationship operations."],
                ),
                RuntimeModuleDescriptor(
                    code="memory_fabric",
                    name="Memory Fabric",
                    role="Combines short-term context, vector recall, lexical recall, and graph facts.",
                    owner_boundary="Python runtime owned",
                    maturity="baseline_live",
                    notes=["Current foundation exists in intelligence and retrieval services."],
                ),
                RuntimeModuleDescriptor(
                    code="persona_engine",
                    name="Persona Engine",
                    role="Builds owner, target, and pair-level structured understanding.",
                    owner_boundary="Python runtime owned",
                    maturity="baseline_live",
                    notes=["Feeds both matchmaker and companion agent flows."],
                ),
                RuntimeModuleDescriptor(
                    code="relationship_strategy_engine",
                    name="Relationship Strategy Engine",
                    role="Generates progression strategy, pacing, and next-best actions.",
                    owner_boundary="Python runtime owned",
                    maturity="baseline_live",
                    notes=["Currently exposed through companion strategy endpoints."],
                ),
                RuntimeModuleDescriptor(
                    code="tool_registry",
                    name="Tool Registry",
                    role="Defines typed actions, schemas, risk tags, and host execution ownership.",
                    owner_boundary="Shared Python and host contract",
                    maturity="partial",
                    notes=["Skills exist today; generalized typed action registry is the next step."],
                ),
                RuntimeModuleDescriptor(
                    code="policy_engine",
                    name="Policy Engine",
                    role="Performs entitlement, pacing, authorization, and budget checks before actioning.",
                    owner_boundary="Shared Python reasoning and Java business truth",
                    maturity="partial",
                    notes=["Host-side policy exists; runtime-side policy abstraction is now formalized."],
                ),
                RuntimeModuleDescriptor(
                    code="channel_gateway",
                    name="Channel Gateway",
                    role="Adapts app chat, moments, notifications, and future external channels.",
                    owner_boundary="Host executor owned",
                    maturity="planned",
                    notes=["OpenClaw-style gateway thinking is useful here, not direct code copying."],
                ),
                RuntimeModuleDescriptor(
                    code="trace_eval_center",
                    name="Trace and Eval Center",
                    role="Stores reasoning traces, retrieval evidence, outcomes, scorecards, and experiments.",
                    owner_boundary="Python runtime owned",
                    maturity="partial",
                    notes=["Trace persistence exists; eval scorecards are the next maturity step."],
                ),
            ],
            core_products=[
                CoreProductDescriptor(
                    code="persona_agent",
                    name="Persona Agent",
                    target_outcome="Produce durable persona and pair intelligence that downstream agents can reuse.",
                    monetization_anchor="Premium persona refreshes and deep relationship reports.",
                    current_state="live_baseline",
                    required_layers=["observe", "recall", "reason", "reflect", "eval"],
                ),
                CoreProductDescriptor(
                    code="matchmaker_agent",
                    name="Matchmaker Agent",
                    target_outcome="Recall, rerank, and explain promising targets while enabling proactive outreach.",
                    monetization_anchor="VIP recommendation packages and proactive outreach flows.",
                    current_state="live_baseline",
                    required_layers=["observe", "recall", "reason", "plan", "policy", "eval"],
                ),
                CoreProductDescriptor(
                    code="companion_agent",
                    name="Companion Agent",
                    target_outcome="Operate the relationship with object-centric chat, pacing, gifts, dates, and follow-up loops.",
                    monetization_anchor="Assistant subscriptions, love-coin usage, and premium automation packs.",
                    current_state="live_baseline",
                    required_layers=["observe", "recall", "reason", "plan", "policy", "act", "reflect", "eval"],
                ),
            ],
            policy_surfaces=[
                PolicySurfaceDescriptor(
                    code=item.code,
                    name=item.name,
                    checks=[
                        PolicyCheckDescriptor(
                            code=check.code,
                            name=check.name,
                            description=check.description,
                            required=check.required,
                        )
                        for check in item.checks
                    ],
                    notes=list(item.notes),
                )
                for item in self.policy_catalog.list_surfaces()
            ],
            eval_metrics=[
                EvalMetricDescriptor(
                    code=item.code,
                    name=item.name,
                    objective=item.objective,
                    data_sources=list(item.data_sources),
                )
                for item in self.eval_catalog.list_metrics()
            ],
            rollout_order=[
                "Phase 1: Formalize product architecture, runtime spine, policy surfaces, and eval metrics in code and docs.",
                "Phase 2: Introduce typed tool registry and runtime policy preflight decisions.",
                "Phase 3: Introduce durable workflow runs and agent-kernel task loops.",
                "Phase 4: Add trace-to-eval scorecards, channel adapters, and commercialization-ready tenant isolation.",
            ],
            docs_path=str(docs_path),
        )

    def _to_descriptor(self, profile: IntegrationProfile) -> IntegrationDescriptor:
        return IntegrationDescriptor(
            code=profile.code,
            name=profile.name,
            role=profile.role,
            selected=profile.selected,
            enabled=profile.enabled,
            installed=profile.installed,
            package_name=profile.package_name,
            docs_url=profile.docs_url,
            notes=list(profile.notes),
        )
