from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field

from agent_runtime.schemas.common import UserCard


class TypedAgentAction(BaseModel):
    action_type: str
    capability_code: str
    title: str
    summary: str
    payload: dict[str, Any] = Field(default_factory=dict)
    risk_level: str = Field(default="low")
    execute_mode: str = Field(default="draft_only")
    requires_approval: bool = False


class ActionDescriptor(BaseModel):
    action_type: str
    capability_code: str
    name: str
    description: str
    owner_boundary: str
    default_execute_mode: str
    default_risk_level: str = Field(default="low")
    policy_surfaces: list[str] = Field(default_factory=list)
    payload_keys: list[str] = Field(default_factory=list)
    notes: list[str] = Field(default_factory=list)


class ActionCatalogResponse(BaseModel):
    actions: list[ActionDescriptor] = Field(default_factory=list)


class ActionPreflightContext(BaseModel):
    vip_active: bool = False
    assistant_enabled: bool = False
    capability_switches: dict[str, bool] = Field(default_factory=dict)
    authorized_target_ids: list[int] = Field(default_factory=list)
    channel_allowed: bool = True
    quiet_hours_active: bool = False
    frequency_limit_reached: bool = False
    love_coin_balance: int = 0
    provider_budget_available: bool = True
    risk_score: float = Field(default=0.0, ge=0.0, le=1.0)


class ActionPreflightDecision(BaseModel):
    action_type: str
    capability_code: str
    decision: str
    effective_execute_mode: str
    failed_checks: list[str] = Field(default_factory=list)
    policy_surfaces: list[str] = Field(default_factory=list)
    reason: str


class CompanionActionPreflightRequest(BaseModel):
    owner: UserCard
    target: UserCard
    actions: list[TypedAgentAction] = Field(default_factory=list)
    policy_context: ActionPreflightContext = Field(default_factory=ActionPreflightContext)


class CompanionActionPreflightResponse(BaseModel):
    allow_auto_execute: bool = False
    decisions: list[ActionPreflightDecision] = Field(default_factory=list)
