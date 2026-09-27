from fastapi.testclient import TestClient

from agent_runtime.app import create_app


def build_payload():
    return {
        "owner": {"user_id": 1, "nickname": "我", "city": "上海", "tags": ["认真恋爱", "真诚表达"]},
        "target": {"user_id": 2, "nickname": "她", "city": "上海", "tags": ["慢热", "安全感", "周末散步"]},
        "recent_messages": [
            {"role": "owner", "content": "今天路过一家安静的小店，感觉你应该会喜欢"},
            {"role": "target", "content": "哈哈我确实更喜欢安静一点的地方"},
            {"role": "owner", "content": "那下次你有空可以跟我说说你平时喜欢怎么放松"},
            {"role": "target", "content": "我一般会散步或者找家舒服的店坐坐"},
        ],
        "behavior_signals": [
            {"name": "stable_chat_days", "value": 3},
            {"name": "conversation_round_count", "value": 10},
            {"name": "positive_reply_ratio", "value": 0.84},
        ],
        "relationship_tags": ["慢热", "认真交往", "同城"],
        "graph_facts": ["target_prefers_safe_and_steady_pacing"],
        "report_goal": "dating_companion",
    }


def test_persona_report_endpoint_returns_structured_persona_kernel():
    client = TestClient(create_app())

    response = client.post("/persona/report/generate", json=build_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["summary"]
    assert body["attachment_style"]
    assert body["core_traits"]

    kernel = body["persona_kernel"]
    assert kernel["stable_traits"]
    assert kernel["expression_style"]
    assert kernel["relationship_style"]
    assert kernel["attachment_style"] == body["attachment_style"]
    assert kernel["love_language"]
    assert kernel["romance_pace"]
    assert kernel["coach_style_candidates"]
    assert kernel["preferred_master_style_code"]
    assert kernel["style_router_reason"]


def test_persona_report_kernel_selected_style_is_in_candidate_pool():
    client = TestClient(create_app())

    response = client.post("/persona/report/generate", json=build_payload())

    assert response.status_code == 200
    kernel = response.json()["persona_kernel"]
    candidate_codes = [item["style_code"] for item in kernel["coach_style_candidates"]]
    assert kernel["preferred_master_style_code"] in candidate_codes
