from agent_runtime.schemas.autonomy import (
    CoachStyleSelectionRequest,
    MatchOpeningSignal,
    ProgramRunRequest,
    RelationshipEvaluateRequest,
)
from agent_runtime.schemas.common import MessageTurn, SignalItem, UserCard
from agent_runtime.schemas.persona import CoachStyleCandidate, PersonaKernel
from agent_runtime.services.coach_style import CoachStyleService
from agent_runtime.services.relationship_state import RelationshipStateService


def build_request() -> RelationshipEvaluateRequest:
    return RelationshipEvaluateRequest(
        owner=UserCard(user_id=1, nickname="我", city="上海", tags=["认真恋爱", "看展", "咖啡"]),
        target=UserCard(user_id=2, nickname="她", city="上海", tags=["慢热", "安全感", "周末散步"]),
        recent_messages=[
            MessageTurn(role="owner", content="今天下班路上看到一家新开的咖啡店"),
            MessageTurn(role="target", content="我最近也在找这种店，周末可能会去试试"),
            MessageTurn(role="owner", content="你是不是偏喜欢安静一点的地方"),
            MessageTurn(role="target", content="对，我不太喜欢太吵的"),
            MessageTurn(role="owner", content="那家店刚好挺安静的"),
            MessageTurn(role="target", content="哈哈那还挺适合我的"),
        ],
        behavior_signals=[
            SignalItem(name="stable_chat_days", value=3),
            SignalItem(name="conversation_round_count", value=14),
            SignalItem(name="positive_reply_ratio", value=0.83),
            SignalItem(name="avg_reply_latency_minutes", value=22),
        ],
        relationship_tags=["慢热", "认真交往", "同城"],
        graph_facts=["target_prefers_safe_and_steady_pacing", "owner_and_target_have_shared_coffee_interest"],
        persona_summary="目标对象慢热但回应稳定，更吃安全感与生活感推进。",
    )


def test_relationship_state_scores_date_ready_and_wechat_ready_from_multi_signal_context():
    service = RelationshipStateService()

    state = service.evaluate(build_request())

    assert state.stage_code in {"warming", "date_ready"}
    assert state.date_ready_score >= 0.55
    assert state.wechat_ready_score >= 0.35
    assert state.master_style_code in {"warm_guardian", "steady_partner", "life_companion"}
    assert state.recommended_next_action in {"reply_send", "date_invite_draft", "proactive_opening_send"}


def test_program_runner_prefers_hongniang_opening_for_recommended_targets_before_chat_starts():
    relationship_service = RelationshipStateService()
    coach_style_service = CoachStyleService()

    request = ProgramRunRequest(
        trigger_code="hongniang_recommendation",
        owner=UserCard(user_id=1, nickname="我", city="上海", tags=["认真恋爱"]),
        target=UserCard(user_id=22, nickname="推荐对象", city="上海", tags=["生活感", "看展"]),
        recent_messages=[],
        behavior_signals=[],
        relationship_tags=["同城", "红娘推荐"],
        persona_summary="目标对象偏生活感、适合轻松真实的切入。",
        match_opening_signal=MatchOpeningSignal(
            match_score=91,
            opening_readiness_score=0.86,
            fit_tags=["同城", "看展"],
            icebreak_openers=["你平时周末会不会去看展或者找家舒服的小店坐坐？"],
            recommended_action="open_chat",
        ),
    )

    state = relationship_service.evaluate_program_seed(request)
    style = coach_style_service.select(
        CoachStyleSelectionRequest(
            owner=request.owner,
            target=request.target,
            relationship_tags=request.relationship_tags,
            graph_facts=[],
            persona_summary=request.persona_summary,
            stage_code=state.stage_code,
            date_ready_score=state.date_ready_score,
            wechat_ready_score=state.wechat_ready_score,
        )
    )
    plan = relationship_service.plan_program(request, state, style)

    assert plan.next_action.action_type == "hongniang_opening_send"
    assert "看展" in plan.next_action.content or "小店" in plan.next_action.content
    assert plan.next_action.master_style_code == style.style_code


def test_program_runner_escalates_to_date_and_wechat_requests_when_thresholds_are_met():
    relationship_service = RelationshipStateService()
    coach_style_service = CoachStyleService()

    request = ProgramRunRequest(
        trigger_code="incoming_message",
        owner=UserCard(user_id=1, nickname="我", city="上海", tags=["认真恋爱"]),
        target=UserCard(user_id=2, nickname="她", city="上海", tags=["慢热", "安全感"]),
        recent_messages=[
            MessageTurn(role="owner", content="最近工作强度是不是也挺大的"),
            MessageTurn(role="target", content="还行，但如果周末能散步就会舒服很多"),
        ],
        behavior_signals=[
            SignalItem(name="stable_chat_days", value=4),
            SignalItem(name="conversation_round_count", value=18),
            SignalItem(name="positive_reply_ratio", value=0.9),
            SignalItem(name="avg_reply_latency_minutes", value=18),
        ],
        relationship_tags=["慢热", "认真交往", "同城"],
        graph_facts=["target_prefers_safe_and_steady_pacing"],
        persona_summary="对方慢热，但已经连续几天稳定承接，安全感建立得不错。",
    )

    state = relationship_service.evaluate_program_seed(request)
    style = coach_style_service.select(
        CoachStyleSelectionRequest(
            owner=request.owner,
            target=request.target,
            relationship_tags=request.relationship_tags,
            graph_facts=request.graph_facts,
            persona_summary=request.persona_summary,
            stage_code=state.stage_code,
            date_ready_score=state.date_ready_score,
            wechat_ready_score=state.wechat_ready_score,
        )
    )
    plan = relationship_service.plan_program(request, state, style)

    assert plan.next_action.action_type in {"date_invite_send", "wechat_request_send"}
    assert plan.next_action.execute_mode == "auto_if_permitted"


def test_program_runner_can_escalate_with_high_intimacy_even_without_fixed_three_day_rule():
    relationship_service = RelationshipStateService()
    coach_style_service = CoachStyleService()

    request = ProgramRunRequest(
        trigger_code="incoming_message",
        owner=UserCard(user_id=1, nickname="我", city="上海", tags=["认真恋爱"]),
        target=UserCard(user_id=2, nickname="她", city="上海", tags=["生活感", "愿意分享"]),
        recent_messages=[
            MessageTurn(role="owner", content="你上次说的那家店我还真去查了"),
            MessageTurn(role="target", content="哈哈你行动力很强耶，我其实挺想找人一起去试试"),
        ],
        behavior_signals=[
            SignalItem(name="stable_chat_days", value=2),
            SignalItem(name="conversation_round_count", value=11),
            SignalItem(name="positive_reply_ratio", value=0.94),
            SignalItem(name="avg_reply_latency_minutes", value=10),
        ],
        relationship_tags=["同城", "互动积极"],
        graph_facts=["target_shows_shared_activity_interest"],
        persona_summary="对方回应热情，互动承接强，适合顺势推进。",
    )

    state = relationship_service.evaluate_program_seed(request)
    style = coach_style_service.select(
        CoachStyleSelectionRequest(
            owner=request.owner,
            target=request.target,
            relationship_tags=request.relationship_tags,
            graph_facts=request.graph_facts,
            persona_summary=request.persona_summary,
            stage_code=state.stage_code,
            date_ready_score=state.date_ready_score,
            wechat_ready_score=state.wechat_ready_score,
        )
    )
    plan = relationship_service.plan_program(request, state, style)

    assert state.intimacy_score >= 0.58
    assert state.progression_score >= 0.55
    assert plan.next_action.action_type in {"date_invite_send", "wechat_request_send", "reply_send"}


def test_program_runner_does_not_escalate_only_because_days_and_rounds_are_high():
    relationship_service = RelationshipStateService()
    coach_style_service = CoachStyleService()

    request = ProgramRunRequest(
        trigger_code="incoming_message",
        owner=UserCard(user_id=1, nickname="我", city="上海", tags=["认真恋爱"]),
        target=UserCard(user_id=2, nickname="她", city="上海", tags=["慢热", "谨慎"]),
        recent_messages=[
            MessageTurn(role="owner", content="今天还好吗"),
            MessageTurn(role="target", content="嗯"),
        ],
        behavior_signals=[
            SignalItem(name="stable_chat_days", value=6),
            SignalItem(name="conversation_round_count", value=22),
            SignalItem(name="positive_reply_ratio", value=0.38),
            SignalItem(name="avg_reply_latency_minutes", value=420),
        ],
        relationship_tags=["慢热"],
        graph_facts=["target_reacts_coldly_to_fast_escalation"],
        persona_summary="虽然已经聊了几天，但对方承接度一般，容易被强推进劝退。",
    )

    state = relationship_service.evaluate_program_seed(request)
    style = coach_style_service.select(
        CoachStyleSelectionRequest(
            owner=request.owner,
            target=request.target,
            relationship_tags=request.relationship_tags,
            graph_facts=request.graph_facts,
            persona_summary=request.persona_summary,
            stage_code=state.stage_code,
            date_ready_score=state.date_ready_score,
            wechat_ready_score=state.wechat_ready_score,
        )
    )
    plan = relationship_service.plan_program(request, state, style)

    assert state.intimacy_score < 0.58
    assert state.risk_score >= 0.25
    assert plan.next_action.action_type == state.recommended_next_action


def test_relationship_state_prefers_persona_kernel_master_style_when_available():
    service = RelationshipStateService()

    state = service.evaluate(
        RelationshipEvaluateRequest(
            owner=UserCard(user_id=1, nickname="我", city="上海", tags=["认真恋爱"]),
            target=UserCard(user_id=2, nickname="她", city="上海", tags=["表达自然", "回应稳定"]),
            recent_messages=[
                MessageTurn(role="owner", content="最近感觉我们的聊天越来越自然了"),
                MessageTurn(role="target", content="嗯，跟你聊着还挺放松的"),
            ],
            behavior_signals=[
                SignalItem(name="stable_chat_days", value=2),
                SignalItem(name="conversation_round_count", value=8),
                SignalItem(name="positive_reply_ratio", value=0.81),
            ],
            relationship_tags=["互动稳定"],
            graph_facts=["target_prefers_reliable_and_grounded_expression"],
            persona_summary="目标对象更适合可靠、稳定、少一点花哨。",
            persona_kernel=PersonaKernel(
                stable_traits=[],
                expression_style="自然、轻松、可靠表达",
                relationship_style="偏稳重承接，更吃兑现感。",
                attachment_style="慢热但回应稳定",
                love_language=["稳定回应", "真诚确认"],
                romance_pace="中速推进，先稳定升温再做下一步。",
                taboo_rules=["不要上来就高压邀约"],
                coach_style_candidates=[
                    CoachStyleCandidate(style_code="steady_partner", style_name="成熟稳重型", fit_score=0.84, reason="最适配当前对象"),
                ],
                preferred_master_style_code="steady_partner",
                style_router_reason="画像内核显示更吃可靠、稳定、少套路的表达。",
            ),
        )
    )

    assert state.master_style_code == "steady_partner"
