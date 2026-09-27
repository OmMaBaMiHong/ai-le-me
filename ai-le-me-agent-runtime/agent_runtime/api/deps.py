from functools import lru_cache

from agent_runtime.core.config import get_settings
from agent_runtime.repositories.runtime_config import RuntimeConfigRepository
from agent_runtime.repositories.trace import RuntimeTraceRepository
from agent_runtime.services.action_preflight import CompanionActionPreflightService
from agent_runtime.services.action_registry import ActionRegistryService
from agent_runtime.services.architecture import RuntimeArchitectureService
from agent_runtime.services.coach_style import CoachStyleService
from agent_runtime.services.companion import CompanionService
from agent_runtime.services.companion_action_planning import CompanionActionPlanningService
from agent_runtime.services.distillation import DistillationService
from agent_runtime.services.evals import AgentEvalCatalog
from agent_runtime.services.intelligence import AgentIntelligenceService
from agent_runtime.services.llm import StructuredLlmService
from agent_runtime.services.matchmaker import SmartMatchService
from agent_runtime.services.persona import PersonaReportService
from agent_runtime.services.policy import AgentPolicyCatalog
from agent_runtime.services.relationship_state import RelationshipStateService
from agent_runtime.services.router import SharedConfigRouter
from agent_runtime.services.trace_store import RuntimeTraceService
from agent_runtime.skills.registry import SkillRegistry, build_default_skill_registry
from agent_runtime.integrations.workflow import WorkflowCatalog
from agent_runtime.integrations.memory import MemoryCatalog
from agent_runtime.integrations.graph import GraphCatalog


@lru_cache(maxsize=1)
def get_config_repository() -> RuntimeConfigRepository:
    return RuntimeConfigRepository()


@lru_cache(maxsize=1)
def get_shared_router() -> SharedConfigRouter:
    return SharedConfigRouter(get_config_repository())


@lru_cache(maxsize=1)
def get_skill_registry() -> SkillRegistry:
    return build_default_skill_registry()


@lru_cache(maxsize=1)
def get_action_registry_service() -> ActionRegistryService:
    return ActionRegistryService()


@lru_cache(maxsize=1)
def get_workflow_catalog() -> WorkflowCatalog:
    return WorkflowCatalog(get_settings())


@lru_cache(maxsize=1)
def get_memory_catalog() -> MemoryCatalog:
    return MemoryCatalog(get_settings())


@lru_cache(maxsize=1)
def get_graph_catalog() -> GraphCatalog:
    return GraphCatalog(get_settings())


@lru_cache(maxsize=1)
def get_policy_catalog() -> AgentPolicyCatalog:
    return AgentPolicyCatalog()


@lru_cache(maxsize=1)
def get_eval_catalog() -> AgentEvalCatalog:
    return AgentEvalCatalog()


@lru_cache(maxsize=1)
def get_agent_intelligence_service() -> AgentIntelligenceService:
    return AgentIntelligenceService(get_settings())


@lru_cache(maxsize=1)
def get_relationship_state_service() -> RelationshipStateService:
    return RelationshipStateService()


@lru_cache(maxsize=1)
def get_coach_style_service() -> CoachStyleService:
    return CoachStyleService()


@lru_cache(maxsize=1)
def get_structured_llm_service() -> StructuredLlmService:
    settings = get_settings()
    return StructuredLlmService(get_shared_router(), timeout_seconds=settings.llm_request_timeout)


@lru_cache(maxsize=1)
def get_trace_repository() -> RuntimeTraceRepository:
    return RuntimeTraceRepository()


@lru_cache(maxsize=1)
def get_runtime_trace_service() -> RuntimeTraceService:
    return RuntimeTraceService(get_trace_repository())


@lru_cache(maxsize=1)
def get_companion_action_preflight_service() -> CompanionActionPreflightService:
    return CompanionActionPreflightService(get_action_registry_service())


@lru_cache(maxsize=1)
def get_companion_action_planning_service() -> CompanionActionPlanningService:
    return CompanionActionPlanningService(get_action_registry_service(), get_companion_action_preflight_service())


@lru_cache(maxsize=1)
def get_persona_service() -> PersonaReportService:
    return PersonaReportService(get_structured_llm_service(), get_agent_intelligence_service(), get_runtime_trace_service())


@lru_cache(maxsize=1)
def get_distillation_service() -> DistillationService:
    return DistillationService()


@lru_cache(maxsize=1)
def get_companion_service() -> CompanionService:
    return CompanionService(
        get_shared_router(),
        get_skill_registry(),
        get_agent_intelligence_service(),
        get_structured_llm_service(),
        get_runtime_trace_service(),
        get_companion_action_planning_service(),
        get_relationship_state_service(),
    )


@lru_cache(maxsize=1)
def get_smart_match_service() -> SmartMatchService:
    return SmartMatchService(
        get_shared_router(),
        get_structured_llm_service(),
        get_agent_intelligence_service(),
        get_runtime_trace_service(),
    )


@lru_cache(maxsize=1)
def get_architecture_service() -> RuntimeArchitectureService:
    return RuntimeArchitectureService(
        settings=get_settings(),
        workflow_catalog=get_workflow_catalog(),
        memory_catalog=get_memory_catalog(),
        graph_catalog=get_graph_catalog(),
        policy_catalog=get_policy_catalog(),
        eval_catalog=get_eval_catalog(),
    )
