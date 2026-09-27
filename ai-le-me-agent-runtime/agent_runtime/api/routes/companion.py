from fastapi import APIRouter, Depends

from agent_runtime.api.deps import (
    get_coach_style_service,
    get_companion_action_preflight_service,
    get_companion_service,
    get_relationship_state_service,
)
from agent_runtime.schemas.action import (
    CompanionActionPreflightRequest,
    CompanionActionPreflightResponse,
)
from agent_runtime.schemas.autonomy import (
    CoachStyleSelectionRequest,
    CoachStyleSelectionResponse,
    ProgramRunRequest,
    ProgramRunResponse,
    RelationshipEvaluateRequest,
    RelationshipEventIngestRequest,
    RelationshipEventIngestResponse,
    RelationshipStateResponse,
)
from agent_runtime.schemas.companion import (
    CompanionGiftPlanRequest,
    CompanionGiftPlanResponse,
    CompanionReplyRequest,
    CompanionReplyResponse,
    CompanionStrategyRequest,
    CompanionStrategyResponse,
)
from agent_runtime.services.coach_style import CoachStyleService
from agent_runtime.services.action_preflight import CompanionActionPreflightService
from agent_runtime.services.companion import CompanionService
from agent_runtime.services.relationship_state import RelationshipStateService

router = APIRouter()


@router.post("/reply/suggest", response_model=CompanionReplyResponse)
def suggest_reply(
    payload: CompanionReplyRequest,
    service: CompanionService = Depends(get_companion_service),
) -> CompanionReplyResponse:
    return service.suggest_reply(payload)


@router.post("/strategy/next-step", response_model=CompanionStrategyResponse)
def next_step_strategy(
    payload: CompanionStrategyRequest,
    service: CompanionService = Depends(get_companion_service),
) -> CompanionStrategyResponse:
    return service.next_step(payload)


@router.post("/gift/plan", response_model=CompanionGiftPlanResponse)
def gift_plan(
    payload: CompanionGiftPlanRequest,
    service: CompanionService = Depends(get_companion_service),
) -> CompanionGiftPlanResponse:
    return service.plan_gift(payload)


@router.post("/actions/preflight", response_model=CompanionActionPreflightResponse)
def companion_action_preflight(
    payload: CompanionActionPreflightRequest,
    service: CompanionActionPreflightService = Depends(get_companion_action_preflight_service),
) -> CompanionActionPreflightResponse:
    return service.evaluate(payload)


@router.post("/relationship/evaluate", response_model=RelationshipStateResponse)
def evaluate_relationship(
    payload: RelationshipEvaluateRequest,
    service: RelationshipStateService = Depends(get_relationship_state_service),
) -> RelationshipStateResponse:
    return service.evaluate(payload)


@router.post("/style/select", response_model=CoachStyleSelectionResponse)
def select_master_style(
    payload: CoachStyleSelectionRequest,
    service: CoachStyleService = Depends(get_coach_style_service),
) -> CoachStyleSelectionResponse:
    return service.select(payload)


@router.post("/program/run", response_model=ProgramRunResponse)
def run_relationship_program(
    payload: ProgramRunRequest,
    relationship_service: RelationshipStateService = Depends(get_relationship_state_service),
    style_service: CoachStyleService = Depends(get_coach_style_service),
) -> ProgramRunResponse:
    state = relationship_service.evaluate_program_seed(payload)
    style = style_service.select(
        CoachStyleSelectionRequest(
            owner=payload.owner,
            target=payload.target,
            relationship_tags=payload.relationship_tags,
            graph_facts=payload.graph_facts,
            persona_summary=payload.persona_summary,
            persona_kernel=payload.persona_kernel,
            stage_code=state.stage_code,
            date_ready_score=state.date_ready_score,
            wechat_ready_score=state.wechat_ready_score,
        )
    )
    return relationship_service.plan_program(payload, state, style)


@router.post("/event/ingest", response_model=RelationshipEventIngestResponse)
def ingest_relationship_event(
    payload: RelationshipEventIngestRequest,
    service: RelationshipStateService = Depends(get_relationship_state_service),
) -> RelationshipEventIngestResponse:
    return service.ingest_event(payload)
