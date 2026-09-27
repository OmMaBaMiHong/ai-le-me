from fastapi.testclient import TestClient

from agent_runtime.app import create_app


def build_payload():
    return {
        "owner": {"user_id": 1, "nickname": "我", "city": "上海", "tags": ["认真恋爱"]},
        "target": {"user_id": 2, "nickname": "她", "city": "上海", "tags": ["慢热", "安全感"]},
        "recent_messages": [
            {"role": "owner", "content": "最近下班是不是也挺累的"},
            {"role": "target", "content": "是有点，不过我会去散步放松"},
        ],
        "behavior_signals": [
            {"name": "stable_chat_days", "value": 3},
            {"name": "conversation_round_count", "value": 12},
        ],
        "relationship_tags": ["慢热", "认真交往"],
        "graph_facts": ["target_prefers_safe_and_steady_pacing"],
        "persona_summary": "目标对象慢热，更适合生活感、低压力推进。",
    }


def test_relationship_evaluate_endpoint_returns_state():
    client = TestClient(create_app())

    response = client.post("/companion/relationship/evaluate", json=build_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["stage_code"]
    assert "date_ready_score" in body
    assert body["master_style_code"]


def test_style_select_endpoint_returns_master_style():
    client = TestClient(create_app())

    response = client.post(
        "/companion/style/select",
        json={
            "owner": {"user_id": 1, "nickname": "我"},
            "target": {"user_id": 2, "nickname": "她", "tags": ["慢热", "安全感"]},
            "relationship_tags": ["慢热", "认真交往"],
            "graph_facts": ["target_prefers_safe_and_steady_pacing"],
            "persona_summary": "目标对象慢热，需要稳定推进。",
            "stage_code": "warming",
            "date_ready_score": 0.42,
            "wechat_ready_score": 0.25,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["style_code"]
    assert body["reasoning_summary"]
