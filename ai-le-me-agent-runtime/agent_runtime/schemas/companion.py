from __future__ import annotations

from typing import Any, Optional

from pydantic import BaseModel, Field

from agent_runtime.schemas.action import ActionPreflightContext, ActionPreflightDecision, TypedAgentAction
from agent_runtime.schemas.common import MessageTurn, SignalItem, UserCard
from agent_runtime.schemas.persona import PersonaKernel


class RelationshipMemory(BaseModel):
    stage: str = Field(default="early")
    known_preferences: list[str] = Field(default_factory=list)
    taboo_topics: list[str] = Field(default_factory=list)
    recent_promises: list[str] = Field(default_factory=list)
    mood_hint: Optional[str] = None


class ReplySuggestion(BaseModel):
    style: str
    text: str
    rationale: str
    risk_level: str


class CompanionReplyRequest(BaseModel):
    owner: UserCard
    target: UserCard
    memory: RelationshipMemory = Field(default_factory=RelationshipMemory)
    recent_messages: list[MessageTurn] = Field(default_factory=list)
    owner_goal: str = Field(default="build_trust")
    desired_tone: str = Field(default="warm")
    available_skills: list[str] = Field(default_factory=list)
    persona_kernel: Optional[PersonaKernel] = None


class CompanionReplyResponse(BaseModel):
    provider: str
    route_profile: Optional[str] = None
    suggestions: list[ReplySuggestion]
    strategy_note: str
    memory_updates: list[str]
    skill_hints: list[str]
    debug_trace_id: Optional[str] = None
    retrieval_hits: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    reasoning_summary: Optional[str] = None


class CompanionStrategyRequest(BaseModel):
    owner: UserCard
    target: UserCard
    memory: RelationshipMemory = Field(default_factory=RelationshipMemory)
    recent_messages: list[MessageTurn] = Field(default_factory=list)
    behavior_signals: list[SignalItem] = Field(default_factory=list)
    objective: str = Field(default="move_relationship_forward")
    policy_context: ActionPreflightContext | None = None
    persona_kernel: Optional[PersonaKernel] = None


class CompanionSuggestedAction(TypedAgentAction):
    pass


class CompanionStrategyResponse(BaseModel):
    provider: str
    route_profile: Optional[str] = None
    relationship_stage: str
    heat_score: float = Field(default=0.0, ge=0.0, le=1.0)
    trust_score: float = Field(default=0.0, ge=0.0, le=1.0)
    tone_warmth: float = Field(default=0.0, ge=0.0, le=1.0)
    intimacy_score: float = Field(default=0.0, ge=0.0, le=1.0)
    progression_score: float = Field(default=0.0, ge=0.0, le=1.0)
    date_ready_score: float = Field(default=0.0, ge=0.0, le=1.0)
    wechat_ready_score: float = Field(default=0.0, ge=0.0, le=1.0)
    risk_score: float = Field(default=0.0, ge=0.0, le=1.0)
    next_best_action: str
    next_24h_plan: list[str]
    guardrails: list[str]
    suggested_actions: list[CompanionSuggestedAction] = Field(default_factory=list)
    should_auto_execute: bool = False
    action_preflight: list[ActionPreflightDecision] = Field(default_factory=list)
    debug_trace_id: Optional[str] = None
    retrieval_hits: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    reasoning_summary: Optional[str] = None


class GiftContext(BaseModel):
    post_id: Optional[int] = None
    target_city: Optional[str] = None
    target_interest: Optional[str] = None


class GiftPlanItem(BaseModel):
    code: str
    name: str
    desc: str
    scene: str
    icon: str
    theme: str
    amount: int
    display_amount: str
    message_draft: str
    rationale: str
    visual_prompt: Optional[str] = None
    motion_prompt: Optional[str] = None
    next_action: str = Field(default="open_chat")
    risk_level: str = Field(default="low")
    recommended: bool = False


class CompanionGiftPlanRequest(BaseModel):
    owner: UserCard
    target: UserCard
    memory: RelationshipMemory = Field(default_factory=RelationshipMemory)
    recent_messages: list[MessageTurn] = Field(default_factory=list)
    behavior_signals: list[SignalItem] = Field(default_factory=list)
    objective: str = Field(default="break_ice")
    scene: str = Field(default="social_intent")
    budget_options: list[int] = Field(default_factory=lambda: [1314, 52100, 66600, 168800])
    gift_context: GiftContext = Field(default_factory=GiftContext)
    persona_kernel: Optional[PersonaKernel] = None


class CompanionGiftPlanResponse(BaseModel):
    provider: str
    route_profile: Optional[str] = None
    scene: str
    relationship_stage: str
    strategy_note: str
    wechat_prompt: str
    gifts: list[GiftPlanItem]
