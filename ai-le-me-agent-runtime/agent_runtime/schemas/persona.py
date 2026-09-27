from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from agent_runtime.schemas.common import MessageTurn, MomentItem, SignalItem, TraitScore, UserCard


class PersonaReportRequest(BaseModel):
    owner: Optional[UserCard] = None
    target: UserCard
    recent_messages: list[MessageTurn] = Field(default_factory=list)
    relationship_tags: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    recent_moments: list[MomentItem] = Field(default_factory=list)
    behavior_signals: list[SignalItem] = Field(default_factory=list)
    report_goal: Optional[str] = Field(default="dating_companion")


class CoachStyleCandidate(BaseModel):
    style_code: str
    style_name: str
    fit_score: float = Field(default=0.0, ge=0.0, le=1.0)
    reason: str


class PersonaKernel(BaseModel):
    stable_traits: list[TraitScore] = Field(default_factory=list)
    expression_style: str
    relationship_style: str
    attachment_style: str
    love_language: list[str] = Field(default_factory=list)
    romance_pace: str
    taboo_rules: list[str] = Field(default_factory=list)
    coach_style_candidates: list[CoachStyleCandidate] = Field(default_factory=list)
    preferred_master_style_code: Optional[str] = None
    style_router_reason: Optional[str] = None


class PersonaReportResponse(BaseModel):
    provider: str
    route_profile: Optional[str] = None
    summary: str
    core_traits: list[TraitScore]
    emotional_style: str
    attachment_style: str
    interest_clusters: list[str]
    risk_flags: list[str]
    approach_suggestions: list[str]
    evidence_digest: list[str]
    debug_trace_id: Optional[str] = None
    retrieval_hits: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    reasoning_summary: Optional[str] = None
    persona_kernel: PersonaKernel
