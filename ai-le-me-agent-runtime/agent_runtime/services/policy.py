from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class PolicyCheck:
    code: str
    name: str
    description: str
    required: bool = True


@dataclass(frozen=True)
class PolicySurface:
    code: str
    name: str
    checks: tuple[PolicyCheck, ...] = field(default_factory=tuple)
    notes: tuple[str, ...] = field(default_factory=tuple)


class AgentPolicyCatalog:
    def list_surfaces(self) -> list[PolicySurface]:
        return [
            PolicySurface(
                code="entitlement",
                name="Entitlement and Packaging",
                checks=(
                    PolicyCheck("vip_entitlement", "VIP entitlement", "Confirms the caller has access to the agent capability."),
                    PolicyCheck("master_switch", "Agent master switch", "Confirms the user enabled the assistant capability."),
                    PolicyCheck("capability_switch", "Capability switch", "Confirms the specific action family is enabled."),
                ),
                notes=(
                    "AiLeMe mode reads VIP and capability truth from Java-owned business state.",
                ),
            ),
            PolicySurface(
                code="target_scope",
                name="Target Scope Authorization",
                checks=(
                    PolicyCheck("target_authorized", "Target authorized", "Confirms the object can be handled by the assistant."),
                    PolicyCheck("channel_allowed", "Channel allowed", "Confirms the selected channel is available for automation."),
                ),
                notes=(
                    "This is compatible with switch-based authorization and object-level allow-listing.",
                ),
            ),
            PolicySurface(
                code="economics",
                name="Economics and Budget",
                checks=(
                    PolicyCheck("love_coin_budget", "Love-coin budget", "Confirms the action stays within billing and quota constraints."),
                    PolicyCheck("provider_budget", "Provider budget", "Confirms external model cost remains inside route budget."),
                ),
                notes=(
                    "This surface is required for external API cost governance.",
                ),
            ),
            PolicySurface(
                code="risk_pacing",
                name="Risk and Pacing",
                checks=(
                    PolicyCheck("quiet_hours", "Quiet hours", "Blocks outreach during configured quiet periods."),
                    PolicyCheck("frequency_limit", "Frequency limit", "Prevents over-contact and aggressive pursuit."),
                    PolicyCheck("risk_grade", "Risk grade", "Requires fallback or escalation for sensitive actions."),
                ),
                notes=(
                    "Risk and pacing should run before any typed action reaches the host executor.",
                ),
            ),
        ]
