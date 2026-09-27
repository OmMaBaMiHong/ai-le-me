from agent_runtime.schemas.action import ActionPreflightContext
from agent_runtime.schemas.common import UserCard
from agent_runtime.schemas.companion import CompanionSuggestedAction
from agent_runtime.services.action_preflight import CompanionActionPreflightService
from agent_runtime.services.action_registry import ActionRegistryService
from agent_runtime.services.companion_action_planning import CompanionActionPlanningService


def test_companion_action_planning_applies_preflight_to_actions():
    registry = ActionRegistryService()
    preflight = CompanionActionPreflightService(registry)
    planner = CompanionActionPlanningService(registry, preflight)

    result = planner.plan(
        owner=UserCard(user_id=1, nickname="我"),
        target=UserCard(user_id=2, nickname="她"),
        actions=[
            CompanionSuggestedAction(
                action_type="message_auto_send",
                capability_code="message.auto.send",
                title="发消息",
                summary="测试",
                payload={"content": "hi"},
                risk_level="low",
                execute_mode="auto_if_permitted",
                requires_approval=False,
            ),
            CompanionSuggestedAction(
                action_type="gift_plan",
                capability_code="gift.plan",
                title="礼物策划",
                summary="测试",
                payload={"objective": "break_ice"},
                risk_level="low",
                execute_mode="draft_only",
                requires_approval=False,
            ),
        ],
        policy_context=ActionPreflightContext(
            vip_active=True,
            assistant_enabled=True,
            capability_switches={"message.auto.send": True, "gift.plan": True},
            authorized_target_ids=[2],
            channel_allowed=True,
            quiet_hours_active=False,
            frequency_limit_reached=False,
            love_coin_balance=5000,
            provider_budget_available=True,
            risk_score=0.12,
        ),
    )

    assert result.allow_auto_execute is True
    assert result.actions[0].execute_mode == "auto_if_permitted"
    assert result.actions[0].requires_approval is False
    assert result.actions[1].execute_mode == "draft_only"
    assert result.decisions[0].decision == "allowed"


def test_companion_action_planning_downgrades_blocked_actions_to_draft():
    registry = ActionRegistryService()
    preflight = CompanionActionPreflightService(registry)
    planner = CompanionActionPlanningService(registry, preflight)

    result = planner.plan(
        owner=UserCard(user_id=1, nickname="我"),
        target=UserCard(user_id=2, nickname="她"),
        actions=[
            CompanionSuggestedAction(
                action_type="message_auto_send",
                capability_code="message.auto.send",
                title="发消息",
                summary="测试",
                payload={"content": "hi"},
                risk_level="low",
                execute_mode="auto_if_permitted",
                requires_approval=False,
            )
        ],
        policy_context=ActionPreflightContext(
            vip_active=False,
            assistant_enabled=False,
            capability_switches={"message.auto.send": False},
            authorized_target_ids=[],
            channel_allowed=True,
            quiet_hours_active=False,
            frequency_limit_reached=False,
            love_coin_balance=0,
            provider_budget_available=True,
            risk_score=0.12,
        ),
    )

    assert result.allow_auto_execute is False
    assert result.actions[0].execute_mode == "draft_only"
    assert result.actions[0].requires_approval is True
    assert "vip_entitlement" in result.decisions[0].failed_checks
