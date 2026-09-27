from fastapi.testclient import TestClient

from agent_runtime.app import create_app


def build_private_person_payload():
    return {
        "scene_type": "private_person_analysis",
        "subject_type": "private_person",
        "relation_label": "ex_partner",
        "analysis_goal": "我想知道为什么每次一吵架就会断联",
        "owner": {"user_id": 1001, "nickname": "我", "city": "上海", "tags": ["认真恋爱", "在意回应"]},
        "subject": {"nickname": "A", "city": "杭州", "tags": ["慢热", "回避冲突"]},
        "answers": [
            {
                "question_code": "conflict_pattern",
                "question_label": "对方吵架后通常会怎样",
                "answer_text": "会先消失一两天，回来像没事一样",
            }
        ],
        "materials": [
            {
                "material_type": "text_note",
                "label": "补充描述",
                "content": "一旦聊到承诺或未来，就会转移话题。",
            },
            {
                "material_type": "chat_screenshot",
                "label": "吵架截图",
                "content": "截图已上传",
                "file_url": "https://cdn.example.com/chat-1.png",
            },
        ],
    }


def build_self_bootstrap_payload():
    return {
        "scene_type": "self_bootstrap",
        "subject_type": "self",
        "analysis_goal": "我想把自我画像做得更准一些",
        "owner": {"user_id": 2001, "nickname": "我", "city": "上海", "tags": ["慢热", "希望认真恋爱"]},
        "subject": {"nickname": "我自己", "tags": ["真诚表达"]},
        "materials": [
            {
                "material_type": "text_note",
                "label": "自述",
                "content": "我需要稳定回应，不喜欢忽冷忽热。",
            }
        ],
    }


def test_distillation_generate_supports_private_person_scene():
    client = TestClient(create_app())

    response = client.post("/distillation/profile/generate", json=build_private_person_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["scene_type"] == "private_person_analysis"
    assert body["summary"]
    assert body["core_insights"]
    assert body["interaction_guidance"]
    assert body["risk_flags"]
    assert body["evidence_cards"]
    assert body["confidence_notes"]
    assert body["persona_kernel"]
    assert body["service_hooks"]["assistant_guardrails"]


def test_distillation_generate_supports_self_bootstrap_scene():
    client = TestClient(create_app())

    response = client.post("/distillation/profile/generate", json=build_self_bootstrap_payload())

    assert response.status_code == 200
    body = response.json()
    assert body["scene_type"] == "self_bootstrap"
    assert body["subject_name"] == "我自己"
    assert any(item["kind"] == "material_signal" for item in body["evidence_cards"])
    assert body["service_hooks"]["profile_copy_hint"]
