from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from agent_runtime.schemas.common import MessageTurn, SignalItem, UserCard
from agent_runtime.schemas.persona import PersonaKernel


class MatchOpeningSignal(BaseModel):
    match_score: int = Field(default=0, ge=0, le=100)
    opening_readiness_score: float = Field(default=0.0, ge=0.0, le=1.0)
    fit_tags: list[str] = Field(default_factory=list)
    icebreak_openers: list[str] = Field(default_factory=list)
    recommended_action: str = Field(default="open_chat")
    do_not_open_reason: Optional[str] = None


class RelationshipEvaluateRequest(BaseModel):
    owner: UserCard
    target: UserCard
    recent_messages: list[MessageTurn] = Field(default_factory=list)
    behavior_signals: list[SignalItem] = Field(default_factory=list)
    relationship_tags: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    persona_summary: str = Field(default="")
    persona_kernel: Optional[PersonaKernel] = None


class RelationshipStateResponse(BaseModel):
    stage_code: str
    heat_score: float = Field(ge=0.0, le=1.0)
    trust_score: float = Field(ge=0.0, le=1.0)
    tone_warmth: float = Field(ge=0.0, le=1.0)
    intimacy_score: float = Field(ge=0.0, le=1.0)
    progression_score: float = Field(ge=0.0, le=1.0)
    date_ready_score: float = Field(ge=0.0, le=1.0)
    wechat_ready_score: float = Field(ge=0.0, le=1.0)
    risk_score: float = Field(ge=0.0, le=1.0)
    master_style_code: str
    recommended_next_action: str
    reasoning_summary: str
    trigger_hints: list[str] = Field(default_factory=list)


class CoachStyleSelectionRequest(BaseModel):
    owner: UserCard
    target: UserCard
    relationship_tags: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    persona_summary: str = Field(default="")
    persona_kernel: Optional[PersonaKernel] = None
    stage_code: str = Field(default="early")
    date_ready_score: float = Field(default=0.0, ge=0.0, le=1.0)
    wechat_ready_score: float = Field(default=0.0, ge=0.0, le=1.0)


class CoachStyleSelectionResponse(BaseModel):
    style_code: str
    style_name: str
    tone_hint: str
    opening_hint: str
    do_not_use_styles: list[str] = Field(default_factory=list)
    reasoning_summary: str


class ProgramSuggestedAction(BaseModel):
    action_type: str
    content: str
    target_user_id: Optional[int] = None
    risk_level: str = Field(default="low")
    requires_approval: bool = False
    master_style_code: str = Field(default="")
    execute_mode: str = Field(default="draft_only")
    payload: dict[str, object] = Field(default_factory=dict)


class ProgramRunRequest(BaseModel):
    trigger_code: str
    owner: UserCard
    target: UserCard
    recent_messages: list[MessageTurn] = Field(default_factory=list)
    behavior_signals: list[SignalItem] = Field(default_factory=list)
    relationship_tags: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    persona_summary: str = Field(default="")
    persona_kernel: Optional[PersonaKernel] = None
    match_opening_signal: Optional[MatchOpeningSignal] = None


class ProgramRunResponse(BaseModel):
    workflow_code: str
    stage_code: str
    master_style_code: str
    next_action: ProgramSuggestedAction
    next_24h_plan: list[str] = Field(default_factory=list)
    reasoning_summary: str


class RelationshipEventIngestRequest(BaseModel):
    event_type: str
    owner: UserCard
    target: UserCard
    event_time: Optional[str] = None
    payload: dict[str, object] = Field(default_factory=dict)


class RelationshipEventIngestResponse(BaseModel):
    accepted: bool = True
    state_hint: Optional[str] = None
    memory_updates: list[str] = Field(default_factory=list)
    graph_updates: list[str] = Field(default_factory=list)
