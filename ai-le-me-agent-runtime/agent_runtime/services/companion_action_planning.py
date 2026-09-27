from __future__ import annotations

from dataclasses import dataclass, field

from agent_runtime.schemas.action import (
    ActionPreflightContext,
    ActionPreflightDecision,
    CompanionActionPreflightRequest,
)
from agent_runtime.schemas.common import UserCard
from agent_runtime.schemas.companion import CompanionSuggestedAction
from agent_runtime.services.action_preflight import CompanionActionPreflightService
from agent_runtime.services.action_registry import ActionRegistryService


@dataclass
class CompanionActionPlanningResult:
    actions: list[CompanionSuggestedAction] = field(default_factory=list)
    decisions: list[ActionPreflightDecision] = field(default_factory=list)
    allow_auto_execute: bool = False


class CompanionActionPlanningService:
    def __init__(
        self,
        action_registry: ActionRegistryService,
        preflight_service: CompanionActionPreflightService,
    ) -> None:
        self.action_registry = action_registry
        self.preflight_service = preflight_service

    def plan(
        self,
        owner: UserCard,
        target: UserCard,
        actions: list[CompanionSuggestedAction],
        policy_context: ActionPreflightContext | None = None,
    ) -> CompanionActionPlanningResult:
        normalized = [
            CompanionSuggestedAction(**self.action_registry.normalize(action).model_dump(mode="json"))
            for action in actions
        ]
        if policy_context is None:
            return CompanionActionPlanningResult(actions=normalized, decisions=[], allow_auto_execute=False)

        preflight = self.preflight_service.evaluate(
            CompanionActionPreflightRequest(
                owner=owner,
                target=target,
                actions=normalized,
                policy_context=policy_context,
            )
        )
        decision_map = {item.action_type: item for item in preflight.decisions}
        adjusted_actions: list[CompanionSuggestedAction] = []
        for action in normalized:
            decision = decision_map.get(action.action_type)
            if decision is None:
                adjusted_actions.append(action)
                continue
            summary = action.summary
            if decision.decision != "allowed":
                summary = f"{action.summary} 当前策略门未放行，将仅保留草稿建议。".strip()
            adjusted_actions.append(
                CompanionSuggestedAction(
                    action_type=action.action_type,
                    capability_code=action.capability_code,
                    title=action.title,
                    summary=summary,
                    payload=action.payload,
                    risk_level=action.risk_level,
                    execute_mode=decision.effective_execute_mode,
                    requires_approval=decision.decision != "allowed" or action.requires_approval,
                )
            )
        return CompanionActionPlanningResult(
            actions=adjusted_actions,
            decisions=preflight.decisions,
            allow_auto_execute=preflight.allow_auto_execute,
        )
