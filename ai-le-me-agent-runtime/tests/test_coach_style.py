from agent_runtime.schemas.autonomy import CoachStyleSelectionRequest
from agent_runtime.schemas.common import UserCard
from agent_runtime.schemas.persona import CoachStyleCandidate, PersonaKernel
from agent_runtime.services.coach_style import CoachStyleService


def test_coach_style_prefers_safe_style_for_slow_warm_persona():
    service = CoachStyleService()

    result = service.select(
        CoachStyleSelectionRequest(
            owner=UserCard(user_id=1, nickname="我"),
            target=UserCard(user_id=2, nickname="她", tags=["慢热", "安全感"]),
            relationship_tags=["认真交往", "慢热"],
            graph_facts=["target_prefers_safe_and_steady_pacing"],
            persona_summary="目标对象慢热，需要稳定回应和真实生活感。",
            stage_code="warming",
            date_ready_score=0.48,
            wechat_ready_score=0.22,
        )
    )

    assert result.style_code in {"warm_guardian", "steady_partner"}
    assert result.reasoning_summary
    assert result.do_not_use_styles


def test_coach_style_can_choose_invitation_driver_when_date_ready_is_high():
    service = CoachStyleService()

    result = service.select(
        CoachStyleSelectionRequest(
            owner=UserCard(user_id=1, nickname="我"),
            target=UserCard(user_id=2, nickname="她", tags=["表达欲高", "同城活动"]),
            relationship_tags=["同城", "互动稳定"],
            graph_facts=["target_responded_well_to_future_plan_talk"],
            persona_summary="目标对象对真实见面有开放性，但不喜欢油腻表达。",
            stage_code="date_ready",
            date_ready_score=0.81,
            wechat_ready_score=0.58,
        )
    )

    assert result.style_code in {"invitation_driver", "life_companion"}
    assert "date_ready" in result.reasoning_summary or "邀约" in result.reasoning_summary


def test_coach_style_prefers_persona_kernel_selected_style_when_signals_do_not_conflict():
    service = CoachStyleService()

    result = service.select(
        CoachStyleSelectionRequest(
            owner=UserCard(user_id=1, nickname="我"),
            target=UserCard(user_id=2, nickname="她", tags=["生活感", "周末散步"]),
            relationship_tags=["同城", "互动稳定"],
            graph_facts=["shared_city_lifestyle_match"],
            persona_summary="目标对象更适合自然的生活感推进。",
            stage_code="warming",
            date_ready_score=0.52,
            wechat_ready_score=0.33,
            persona_kernel=PersonaKernel(
                stable_traits=[],
                expression_style="自然、短句、生活感表达",
                relationship_style="更适合生活感、真实感、轻陪伴式推进。",
                attachment_style="慢热但可稳定升温",
                love_language=["陪伴相处", "稳定回应"],
                romance_pace="中速推进，先从日常共鸣建立熟悉感。",
                taboo_rules=["不要油腻输出"],
                coach_style_candidates=[
                    CoachStyleCandidate(style_code="life_companion", style_name="生活感陪伴型", fit_score=0.86, reason="最贴近关系风格"),
                    CoachStyleCandidate(style_code="steady_partner", style_name="成熟稳重型", fit_score=0.68, reason="可作为次选"),
                ],
                preferred_master_style_code="life_companion",
                style_router_reason="目标对象吃生活感陪伴，不适合套路化暧昧表达。",
            ),
        )
    )

    assert result.style_code == "life_companion"
    assert "persona_kernel" in result.reasoning_summary
