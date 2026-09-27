from __future__ import annotations

from agent_runtime.schemas.action import (
    ActionPreflightDecision,
    CompanionActionPreflightRequest,
    CompanionActionPreflightResponse,
)
from agent_runtime.services.action_registry import ActionRegistryService


class CompanionActionPreflightService:
    def __init__(self, action_registry: ActionRegistryService) -> None:
        self.action_registry = action_registry

    def evaluate(self, payload: CompanionActionPreflightRequest) -> CompanionActionPreflightResponse:
        decisions: list[ActionPreflightDecision] = []
        for action in payload.actions:
            normalized = self.action_registry.normalize(action)
            descriptor = self.action_registry.get(normalized.action_type)
            if descriptor is None:
                decisions.append(
                    ActionPreflightDecision(
                        action_type=normalized.action_type,
                        capability_code=normalized.capability_code,
                        decision="blocked",
                        effective_execute_mode="draft_only",
                        failed_checks=["unknown_action"],
                        policy_surfaces=[],
                        reason="Unknown action type is not executable.",
                    )
                )
                continue

            failed_checks: list[str] = []
            context = payload.policy_context
            target_id = payload.target.user_id

            if "entitlement" in descriptor.policy_surfaces:
                if not context.vip_active:
                    failed_checks.append("vip_entitlement")
                if not context.assistant_enabled:
                    failed_checks.append("master_switch")
                if not context.capability_switches.get(normalized.capability_code, False):
                    failed_checks.append("capability_switch")

            if "target_scope" in descriptor.policy_surfaces:
                if target_id is not None and target_id not in context.authorized_target_ids:
                    failed_checks.append("target_authorized")
                if not context.channel_allowed:
                    failed_checks.append("channel_allowed")

            if "risk_pacing" in descriptor.policy_surfaces:
                if context.quiet_hours_active:
                    failed_checks.append("quiet_hours")
                if context.frequency_limit_reached:
                    failed_checks.append("frequency_limit")
                if context.risk_score >= 0.65 or normalized.requires_approval:
                    failed_checks.append("risk_grade")

            decision = "allowed" if not failed_checks else "blocked"
            effective_execute_mode = normalized.execute_mode if decision == "allowed" else "draft_only"
            reason = "Policy checks passed." if decision == "allowed" else "Blocked by policy preflight checks."
            decisions.append(
                ActionPreflightDecision(
                    action_type=normalized.action_type,
                    capability_code=normalized.capability_code,
                    decision=decision,
                    effective_execute_mode=effective_execute_mode,
                    failed_checks=failed_checks,
                    policy_surfaces=list(descriptor.policy_surfaces),
                    reason=reason,
                )
            )

        return CompanionActionPreflightResponse(
            allow_auto_execute=(
                bool(decisions)
                and all(item.decision == "allowed" for item in decisions)
                and any(item.effective_execute_mode == "auto_if_permitted" for item in decisions)
            ),
            decisions=decisions,
        )
