from fastapi.testclient import TestClient

from agent_runtime.app import create_app


def test_runtime_action_catalog_exposes_typed_actions():
    client = TestClient(create_app())

    response = client.get("/runtime/actions/catalog")

    assert response.status_code == 200
    body = response.json()
    actions = {item["action_type"]: item for item in body["actions"]}
    assert "message_auto_send" in actions
    assert "gift_plan" in actions
    assert actions["message_auto_send"]["default_execute_mode"] == "auto_if_permitted"
    assert "entitlement" in actions["message_auto_send"]["policy_surfaces"]
    assert "risk_pacing" in actions["message_auto_send"]["policy_surfaces"]


def test_companion_action_preflight_blocks_when_policy_fails():
    client = TestClient(create_app())

    response = client.post(
        "/companion/actions/preflight",
        json={
            "owner": {"user_id": 1, "nickname": "我"},
            "target": {"user_id": 2, "nickname": "她"},
            "actions": [
                {
                    "action_type": "message_auto_send",
                    "capability_code": "message.auto.send",
                    "title": "发消息",
                    "summary": "测试",
                    "payload": {"content": "晚上好"},
                    "risk_level": "low",
                    "execute_mode": "auto_if_permitted",
                    "requires_approval": False,
                }
            ],
            "policy_context": {
                "vip_active": False,
                "assistant_enabled": False,
                "capability_switches": {"message.auto.send": False},
                "authorized_target_ids": [],
                "channel_allowed": True,
                "quiet_hours_active": False,
                "frequency_limit_reached": False,
                "love_coin_balance": 0,
                "provider_budget_available": True,
                "risk_score": 0.2,
            },
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["allow_auto_execute"] is False
    decision = body["decisions"][0]
    assert decision["action_type"] == "message_auto_send"
    assert decision["decision"] == "blocked"
    assert "vip_entitlement" in decision["failed_checks"]
    assert "master_switch" in decision["failed_checks"]
    assert "target_authorized" in decision["failed_checks"]
    assert decision["effective_execute_mode"] == "draft_only"


def test_companion_action_preflight_allows_safe_actions_when_context_is_ready():
    client = TestClient(create_app())

    response = client.post(
        "/companion/actions/preflight",
        json={
            "owner": {"user_id": 1, "nickname": "我"},
            "target": {"user_id": 2, "nickname": "她"},
            "actions": [
                {
                    "action_type": "message_auto_send",
                    "capability_code": "message.auto.send",
                    "title": "发消息",
                    "summary": "测试",
                    "payload": {"content": "晚上好"},
                    "risk_level": "low",
                    "execute_mode": "auto_if_permitted",
                    "requires_approval": False,
                },
                {
                    "action_type": "gift_plan",
                    "capability_code": "gift.plan",
                    "title": "礼物策划",
                    "summary": "测试",
                    "payload": {"objective": "break_ice"},
                    "risk_level": "low",
                    "execute_mode": "draft_only",
                    "requires_approval": False,
                },
            ],
            "policy_context": {
                "vip_active": True,
                "assistant_enabled": True,
                "capability_switches": {"message.auto.send": True, "gift.plan": True},
                "authorized_target_ids": [2],
                "channel_allowed": True,
                "quiet_hours_active": False,
                "frequency_limit_reached": False,
                "love_coin_balance": 5000,
                "provider_budget_available": True,
                "risk_score": 0.18,
            },
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["allow_auto_execute"] is True
    decisions = {item["action_type"]: item for item in body["decisions"]}
    assert decisions["message_auto_send"]["decision"] == "allowed"
    assert decisions["message_auto_send"]["effective_execute_mode"] == "auto_if_permitted"
    assert decisions["gift_plan"]["decision"] == "allowed"
    assert decisions["gift_plan"]["effective_execute_mode"] == "draft_only"


def test_companion_next_step_exposes_relationship_signal_scores():
    client = TestClient(create_app())

    response = client.post(
        "/companion/strategy/next-step",
        json={
            "owner": {"user_id": 1, "nickname": "我"},
            "target": {"user_id": 2, "nickname": "她", "tags": ["慢热", "安全感"]},
            "recent_messages": [
                {"role": "owner", "content": "你上次提到的那家店我去搜了下"},
                {"role": "target", "content": "哈哈真的嘛，我其实挺想找个舒服的人一起去试试"},
            ],
            "behavior_signals": [
                {"name": "stable_chat_days", "value": 2},
                {"name": "conversation_round_count", "value": 11},
                {"name": "positive_reply_ratio", "value": 0.9},
                {"name": "avg_reply_latency_minutes", "value": 18},
            ],
            "objective": "move_relationship_forward",
            "policy_context": {
                "vip_active": True,
                "assistant_enabled": True,
                "capability_switches": {"message.auto.send": True, "gift.plan": True},
                "authorized_target_ids": [2],
                "channel_allowed": True,
                "quiet_hours_active": False,
                "frequency_limit_reached": False,
                "love_coin_balance": 5000,
                "provider_budget_available": True,
                "risk_score": 0.18,
            },
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["intimacy_score"] >= 0.0
    assert body["progression_score"] >= 0.0
    assert body["tone_warmth"] >= 0.0
