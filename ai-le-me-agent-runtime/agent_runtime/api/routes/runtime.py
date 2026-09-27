from fastapi import APIRouter, Depends, HTTPException, Query

from agent_runtime.api.deps import (
    get_action_registry_service,
    get_architecture_service,
    get_runtime_trace_service,
    get_shared_router,
    get_skill_registry,
)
from agent_runtime.schemas.action import ActionCatalogResponse
from agent_runtime.schemas.runtime import RuntimeArchitectureSnapshot, RuntimeConfigSnapshot, SkillCatalogResponse
from agent_runtime.schemas.trace import RuntimeTraceDetail, RuntimeTracePageResponse
from agent_runtime.services.architecture import RuntimeArchitectureService
from agent_runtime.services.action_registry import ActionRegistryService
from agent_runtime.services.router import SharedConfigRouter
from agent_runtime.services.trace_store import RuntimeTraceService
from agent_runtime.skills.registry import SkillRegistry

router = APIRouter()


@router.get("/config/current", response_model=RuntimeConfigSnapshot)
def current_runtime_config(router_service: SharedConfigRouter = Depends(get_shared_router)) -> RuntimeConfigSnapshot:
    return router_service.snapshot()


@router.get("/skills", response_model=SkillCatalogResponse)
def runtime_skills(skill_registry: SkillRegistry = Depends(get_skill_registry)) -> SkillCatalogResponse:
    return SkillCatalogResponse(skills=skill_registry.list_skills())


@router.get("/actions/catalog", response_model=ActionCatalogResponse)
def runtime_action_catalog(
    action_registry: ActionRegistryService = Depends(get_action_registry_service),
) -> ActionCatalogResponse:
    return action_registry.catalog()


@router.get("/architecture", response_model=RuntimeArchitectureSnapshot)
def runtime_architecture(
    architecture_service: RuntimeArchitectureService = Depends(get_architecture_service),
) -> RuntimeArchitectureSnapshot:
    return architecture_service.snapshot()


@router.get("/traces", response_model=RuntimeTracePageResponse)
def runtime_traces(
    agent_type: str | None = Query(default=None),
    owner_user_id: int | None = Query(default=None),
    target_user_id: int | None = Query(default=None),
    provider_code: str | None = Query(default=None),
    trace_id: str | None = Query(default=None),
    keyword: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    trace_service: RuntimeTraceService = Depends(get_runtime_trace_service),
) -> RuntimeTracePageResponse:
    return trace_service.list_traces(
        agent_type=agent_type,
        owner_user_id=owner_user_id,
        target_user_id=target_user_id,
        provider_code=provider_code,
        trace_id=trace_id,
        keyword=keyword,
        page=page,
        page_size=page_size,
    )


@router.get("/traces/{trace_id}", response_model=RuntimeTraceDetail)
def runtime_trace_detail(
    trace_id: str,
    trace_service: RuntimeTraceService = Depends(get_runtime_trace_service),
) -> RuntimeTraceDetail:
    trace = trace_service.get_trace(trace_id)
    if trace is None:
        raise HTTPException(status_code=404, detail="Trace not found")
    return trace
