from __future__ import annotations

from agent_runtime.schemas.action import ActionCatalogResponse, ActionDescriptor, TypedAgentAction


class ActionRegistryService:
    def __init__(self) -> None:
        self._actions = {
            item.action_type: item
            for item in [
                ActionDescriptor(
                    action_type="message_auto_send",
                    capability_code="message.auto.send",
                    name="Message Auto Send",
                    description="Send or queue a relationship-preserving chat message through the host app.",
                    owner_boundary="Host chat executor",
                    default_execute_mode="auto_if_permitted",
                    default_risk_level="low",
                    policy_surfaces=["entitlement", "target_scope", "risk_pacing"],
                    payload_keys=["content", "source"],
                    notes=["Primary proactive chat action for the companion agent."],
                ),
                ActionDescriptor(
                    action_type="gift_plan",
                    capability_code="gift.plan",
                    name="Gift Plan",
                    description="Generate a gift strategy or draft plan before any actual spend occurs.",
                    owner_boundary="Runtime planning with host gift execution later",
                    default_execute_mode="draft_only",
                    default_risk_level="low",
                    policy_surfaces=["entitlement", "target_scope"],
                    payload_keys=["objective", "scene"],
                    notes=["Planning is allowed before actual gift execution is introduced as a typed action."],
                ),
                ActionDescriptor(
                    action_type="date_invite_send",
                    capability_code="date.invite.send",
                    name="Date Invite Send",
                    description="Send a concrete date invitation or scheduling proposal to the target.",
                    owner_boundary="Host chat and schedule executor",
                    default_execute_mode="draft_only",
                    default_risk_level="medium",
                    policy_surfaces=["entitlement", "target_scope", "risk_pacing"],
                    payload_keys=["content", "time_slots"],
                    notes=["Should remain draft-first until date-specific governance is fully implemented."],
                ),
                ActionDescriptor(
                    action_type="wechat_request_send",
                    capability_code="contact.wechat.request",
                    name="WeChat Request Send",
                    description="Request contact exchange when relationship readiness is sufficient.",
                    owner_boundary="Host chat executor",
                    default_execute_mode="draft_only",
                    default_risk_level="medium",
                    policy_surfaces=["entitlement", "target_scope", "risk_pacing"],
                    payload_keys=["content"],
                    notes=["Contact exchange is sensitive and should stay policy-bound."],
                ),
                ActionDescriptor(
                    action_type="moment_draft_create",
                    capability_code="moment.draft.create",
                    name="Moment Draft Create",
                    description="Draft a moment or post using relationship context and approved media.",
                    owner_boundary="Runtime drafting with host publish executor",
                    default_execute_mode="draft_only",
                    default_risk_level="low",
                    policy_surfaces=["entitlement"],
                    payload_keys=["caption", "media_ids"],
                    notes=["Media authorization checks should run in host policy hooks."],
                ),
            ]
        }

    def catalog(self) -> ActionCatalogResponse:
        return ActionCatalogResponse(actions=list(self._actions.values()))

    def list_actions(self) -> list[ActionDescriptor]:
        return list(self._actions.values())

    def get(self, action_type: str) -> ActionDescriptor | None:
        return self._actions.get(action_type)

    def allowed_action_types(self) -> set[str]:
        return set(self._actions.keys())

    def normalize(self, action: TypedAgentAction) -> TypedAgentAction:
        descriptor = self.get(action.action_type)
        if descriptor is None:
            return action
        return TypedAgentAction(
            action_type=descriptor.action_type,
            capability_code=action.capability_code or descriptor.capability_code,
            title=action.title,
            summary=action.summary,
            payload=action.payload,
            risk_level=action.risk_level or descriptor.default_risk_level,
            execute_mode=action.execute_mode or descriptor.default_execute_mode,
            requires_approval=action.requires_approval,
        )
