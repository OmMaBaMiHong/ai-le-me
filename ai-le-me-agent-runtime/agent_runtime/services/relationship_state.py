from __future__ import annotations

from agent_runtime.schemas.autonomy import (
    CoachStyleSelectionResponse,
    ProgramRunRequest,
    ProgramRunResponse,
    ProgramSuggestedAction,
    RelationshipEvaluateRequest,
    RelationshipEventIngestRequest,
    RelationshipEventIngestResponse,
    RelationshipStateResponse,
)


class RelationshipStateService:
    def evaluate(self, request: RelationshipEvaluateRequest) -> RelationshipStateResponse:
        signals = {item.name: item.value for item in request.behavior_signals}
        kernel_text = self._persona_kernel_text(request)
        round_count = self._to_float(signals.get("conversation_round_count"), len(request.recent_messages))
        stable_days = self._to_float(signals.get("stable_chat_days"), 0)
        reply_latency = self._to_float(signals.get("avg_reply_latency_minutes"), 180)
        positive_ratio = self._to_float(signals.get("positive_reply_ratio"), 0.45)
        tone_warmth = self._to_float(signals.get("tone_warmth_score"), self._infer_tone_warmth(request))
        message_density = min(1.0, len(request.recent_messages) / 12.0)
        graph_affinity = self._graph_affinity_score(request.graph_facts)
        engagement_score = self._clamp(
            positive_ratio * 0.42
            + tone_warmth * 0.24
            + min(round_count / 20.0, 0.16)
            + min(stable_days / 7.0, 0.1)
            + message_density * 0.08
            - min(reply_latency / 720.0, 0.12)
        )

        heat_score = self._clamp(
            0.12
            + stable_days * 0.04
            + round_count * 0.012
            + positive_ratio * 0.28
            + tone_warmth * 0.16
            + message_density * 0.1
            + max(0.0, graph_affinity) * 0.08
            - min(reply_latency / 600.0, 0.22)
        )
        trust_score = self._clamp(
            0.16
            + positive_ratio * 0.3
            + tone_warmth * 0.18
            + min(stable_days / 10.0, 0.14)
            + graph_affinity * 0.14
            + (0.06 if request.persona_summary else 0.0)
            + (0.04 if kernel_text else 0.0)
        )
        risk_score = self._clamp(
            0.32
            - positive_ratio * 0.15
            - tone_warmth * 0.1
            - min(stable_days / 10.0, 0.06)
            + (0.1 if reply_latency > 360 else 0.0)
            + (0.08 if any("拒绝" in text or "冷淡" in text for text in request.graph_facts) else 0.0)
        )
        intimacy_score = self._clamp(
            heat_score * 0.24
            + trust_score * 0.28
            + engagement_score * 0.28
            + tone_warmth * 0.12
            + min(stable_days / 10.0, 0.05)
            - risk_score * 0.18
        )
        date_ready = self._clamp(
            heat_score * 0.36
            + trust_score * 0.32
            + engagement_score * 0.16
            + intimacy_score * 0.12
            - risk_score * 0.28
        )
        wechat_ready = self._clamp(
            trust_score * 0.34
            + intimacy_score * 0.24
            + date_ready * 0.2
            + tone_warmth * 0.08
            - risk_score * 0.2
        )
        progression_score = self._clamp(
            intimacy_score * 0.4
            + date_ready * 0.24
            + wechat_ready * 0.14
            + min(round_count / 24.0, 0.1)
            + min(stable_days / 10.0, 0.06)
            - risk_score * 0.16
        )

        stage_code = "early"
        if progression_score >= 0.7 and date_ready >= 0.64:
            stage_code = "date_ready"
        elif intimacy_score >= 0.48 and trust_score >= 0.42:
            stage_code = "warming"

        master_style_code = self._resolve_master_style(request, stage_code, date_ready, wechat_ready)
        next_action = "reply_send"
        if stage_code == "date_ready":
            next_action = "date_invite_draft"
        elif round_count == 0:
            next_action = "proactive_opening_send"

        return RelationshipStateResponse(
            stage_code=stage_code,
            heat_score=round(heat_score, 4),
            trust_score=round(trust_score, 4),
            tone_warmth=round(tone_warmth, 4),
            intimacy_score=round(intimacy_score, 4),
            progression_score=round(progression_score, 4),
            date_ready_score=round(date_ready, 4),
            wechat_ready_score=round(wechat_ready, 4),
            risk_score=round(risk_score, 4),
            master_style_code=master_style_code,
            recommended_next_action=next_action,
            reasoning_summary=(
                f"signals: stable_days={stable_days:.1f}, rounds={round_count:.0f}, "
                f"positive_ratio={positive_ratio:.2f}, tone={tone_warmth:.2f}, "
                f"intimacy={intimacy_score:.2f}, progression={progression_score:.2f}, latency={reply_latency:.0f}m"
                f"{'; persona_kernel=on' if kernel_text else ''}"
            ),
            trigger_hints=self._trigger_hints(intimacy_score, progression_score, date_ready, wechat_ready, tone_warmth),
        )

    def evaluate_program_seed(self, request: ProgramRunRequest) -> RelationshipStateResponse:
        eval_request = RelationshipEvaluateRequest(
            owner=request.owner,
            target=request.target,
            recent_messages=request.recent_messages,
            behavior_signals=request.behavior_signals,
            relationship_tags=request.relationship_tags,
            graph_facts=request.graph_facts,
            persona_summary=request.persona_summary,
            persona_kernel=request.persona_kernel,
        )
        state = self.evaluate(eval_request)
        if request.trigger_code == "hongniang_recommendation" and request.match_opening_signal:
            opening_boost = self._clamp(request.match_opening_signal.opening_readiness_score)
            boosted_date_ready = self._clamp(state.date_ready_score * 0.4 + opening_boost * 0.35)
            return RelationshipStateResponse(
                **{
                    **state.model_dump(mode="json"),
                    "stage_code": "early",
                    "date_ready_score": round(boosted_date_ready, 4),
                    "recommended_next_action": "hongniang_opening_send",
                    "reasoning_summary": f"{state.reasoning_summary}; hongniang_opening={opening_boost:.2f}",
                }
            )
        return state

    def plan_program(
        self,
        request: ProgramRunRequest,
        state: RelationshipStateResponse,
        style: CoachStyleSelectionResponse,
    ) -> ProgramRunResponse:
        action_type = self._resolve_program_action(request, state)
        execute_mode = "auto_if_permitted" if action_type.endswith("_send") else "draft_only"
        risk_level = "low" if state.risk_score < 0.35 else "medium"
        content = self._build_content(request, state, style, action_type)
        if request.trigger_code == "hongniang_recommendation":
            action_type = "hongniang_opening_send"
            execute_mode = "auto_if_permitted"
            content = self._build_hongniang_opening(request, style)
        next_plan = self._build_next_plan(request, state, action_type)
        return ProgramRunResponse(
            workflow_code="relationship_program",
            stage_code=state.stage_code,
            master_style_code=style.style_code,
            next_action=ProgramSuggestedAction(
                action_type=action_type,
                content=content,
                target_user_id=request.target.user_id,
                risk_level=risk_level,
                requires_approval=state.risk_score >= 0.65,
                master_style_code=style.style_code,
                execute_mode=execute_mode,
                payload={"trigger_code": request.trigger_code, "stage_code": state.stage_code},
            ),
            next_24h_plan=next_plan,
            reasoning_summary=f"{state.reasoning_summary}; style={style.style_code}",
        )

    def ingest_event(self, request: RelationshipEventIngestRequest) -> RelationshipEventIngestResponse:
        event_type = request.event_type.strip().lower()
        memory_updates: list[str] = []
        graph_updates: list[str] = []
        state_hint = "observe"
        if event_type == "incoming_message":
            memory_updates.append("refresh_recent_message_window")
            graph_updates.append("append_message_interaction_edge")
            state_hint = "reply_window_open"
        elif event_type == "date_invite_accepted":
            memory_updates.append("relationship_stage_upgrade=date_proposed")
            graph_updates.append("append_positive_date_signal")
            state_hint = "date_proposed"
        elif event_type == "wechat_exchange_confirmed":
            memory_updates.append("relationship_stage_upgrade=wechat_exchanged")
            graph_updates.append("append_contact_exchange_signal")
            state_hint = "wechat_exchanged"
        return RelationshipEventIngestResponse(
            accepted=True,
            state_hint=state_hint,
            memory_updates=memory_updates,
            graph_updates=graph_updates,
        )

    def _resolve_master_style(
        self,
        request: RelationshipEvaluateRequest,
        stage_code: str,
        date_ready: float,
        wechat_ready: float,
    ) -> str:
        preferred_style = request.persona_kernel.preferred_master_style_code if request.persona_kernel else ""
        text = f"{request.persona_summary} {self._persona_kernel_text(request)} {' '.join(request.relationship_tags)} {' '.join(request.graph_facts)} {' '.join(request.target.tags)}"
        if preferred_style in {"warm_guardian", "steady_partner", "life_companion", "invitation_driver", "playful_tease"}:
            return preferred_style
        if "慢热" in text or "安全感" in text:
            return "warm_guardian"
        if stage_code == "date_ready":
            return "life_companion" if date_ready < 0.82 else "invitation_driver"
        if wechat_ready > 0.56:
            return "steady_partner"
        return "life_companion"

    def _persona_kernel_text(self, request: RelationshipEvaluateRequest) -> str:
        kernel = request.persona_kernel
        if kernel is None:
            return ""
        parts = [
            kernel.expression_style,
            kernel.relationship_style,
            kernel.attachment_style,
            kernel.romance_pace,
            " ".join(kernel.love_language),
            " ".join(kernel.taboo_rules),
            kernel.style_router_reason or "",
        ]
        return " ".join(part.strip() for part in parts if part and part.strip())

    def _trigger_hints(
        self,
        intimacy_score: float,
        progression_score: float,
        date_ready: float,
        wechat_ready: float,
        tone_warmth: float,
    ) -> list[str]:
        hints: list[str] = []
        if tone_warmth >= 0.62:
            hints.append("warm_tone_window")
        if intimacy_score >= 0.58:
            hints.append("intimacy_threshold_reached")
        if progression_score >= 0.55:
            hints.append("progression_threshold_reached")
        if date_ready >= 0.7:
            hints.append("date_ready_window")
        if wechat_ready >= 0.6:
            hints.append("wechat_ready_window")
        return hints

    def _build_hongniang_opening(self, request: ProgramRunRequest, style: CoachStyleSelectionResponse) -> str:
        signal = request.match_opening_signal
        if signal and signal.icebreak_openers:
            return signal.icebreak_openers[0]
        fit = ""
        if signal and signal.fit_tags:
            fit = signal.fit_tags[0]
        target_name = request.target.nickname or "你"
        if fit:
            return f"看到你也关注 {fit} 这类内容，突然觉得我们可能会聊得来，你平时会怎么安排这种小爱好？"
        return f"嗨，看到你的资料有种很自然的生活感，{target_name} 平时周末更喜欢安静放松还是到处走走？"

    def _build_content(
        self,
        request: ProgramRunRequest,
        state: RelationshipStateResponse,
        style: CoachStyleSelectionResponse,
        action_type: str,
    ) -> str:
        target_name = request.target.nickname or "你"
        if action_type == "wechat_request_send":
            return f"这几天跟 {target_name} 聊下来我感觉挺舒服的，如果你也觉得现在的节奏不错，我们要不要加个微信，之后联系会更方便一点？"
        if action_type in {"date_invite_draft", "date_invite_send"}:
            return f"这几天跟 {target_name} 聊得挺舒服的，如果你也愿意，我们可以找个轻松一点的时间喝杯咖啡或者散散步。"
        if style.style_code == "warm_guardian":
            return f"你上次提到想找个安静一点的节奏，我记住了。最近有没有哪件小事让你觉得轻松一点？"
        if style.style_code == "playful_tease":
            return f"感觉你对生活里的小确幸应该挺有研究，不然怎么总能让我想到一些轻松的好心情。"
        return f"你之前提到的那个话题我还记着，最近如果你有空，我还挺想继续听你讲讲。"

    def _build_next_plan(self, request: ProgramRunRequest, state: RelationshipStateResponse, action_type: str) -> list[str]:
        if request.trigger_code == "hongniang_recommendation":
            return [
                "先用红娘推荐的一条轻破冰消息打开对话。",
                "如果 24 小时内对方承接，再切入共同兴趣继续聊。",
                "若未回复，不做连续追击，等待下一触发窗口。",
            ]
        if action_type == "wechat_request_send":
            return [
                "顺势提出交换微信，不额外堆砌压力。",
                "如果对方犹豫，回到轻松聊天，给对方安全感。",
                "若对方同意，再把联系节奏切到更稳定的日常互动。",
            ]
        if action_type in {"date_invite_draft", "date_invite_send"} or state.stage_code == "date_ready":
            return [
                "先维持当前聊天顺滑感。",
                "在对方活跃窗口给一个低压邀约。",
                "若对方模糊回避，回到轻松话题，不继续逼近。",
            ]
        return [
            "延续最近一轮相对顺的内容。",
            "围绕生活感或共同兴趣加一点真实表达。",
            "观察对方承接度，再决定是否推进下一步。",
        ]

    def _resolve_program_action(self, request: ProgramRunRequest, state: RelationshipStateResponse) -> str:
        if request.trigger_code == "hongniang_recommendation":
            return "hongniang_opening_send"
        signals = {item.name: item.value for item in request.behavior_signals}
        stable_days = self._to_float(signals.get("stable_chat_days"), 0)
        round_count = self._to_float(signals.get("conversation_round_count"), len(request.recent_messages))
        positive_ratio = self._to_float(signals.get("positive_reply_ratio"), 0.45)
        tone_warmth = self._to_float(signals.get("tone_warmth_score"), self._infer_program_tone_warmth(request))

        if self._should_request_wechat(request, state, stable_days, round_count, positive_ratio, tone_warmth):
            return "wechat_request_send"
        if self._should_send_date_invite(request, state, stable_days, round_count, positive_ratio, tone_warmth):
            return "date_invite_send"
        return state.recommended_next_action

    def _should_send_date_invite(
        self,
        request: ProgramRunRequest,
        state: RelationshipStateResponse,
        stable_days: float,
        round_count: float,
        positive_ratio: float,
        tone_warmth: float,
    ) -> bool:
        slow_burn = self._is_slow_burn_target_text(
            " ".join(request.relationship_tags),
            " ".join(request.graph_facts),
            request.persona_summary,
            " ".join(request.target.tags),
        )
        intimacy_threshold = 0.62 + (0.04 if slow_burn else 0.0) + max(0.0, state.risk_score - 0.28) * 0.12
        progression_threshold = 0.58 + (0.03 if slow_burn else 0.0)
        return (
            state.intimacy_score >= intimacy_threshold
            and state.progression_score >= progression_threshold
            and state.date_ready_score >= 0.64
            and state.risk_score < 0.5
            and (positive_ratio >= 0.62 or tone_warmth >= 0.66)
            and (round_count >= 8 or stable_days >= 2)
        )

    def _should_request_wechat(
        self,
        request: ProgramRunRequest,
        state: RelationshipStateResponse,
        stable_days: float,
        round_count: float,
        positive_ratio: float,
        tone_warmth: float,
    ) -> bool:
        slow_burn = self._is_slow_burn_target_text(
            " ".join(request.relationship_tags),
            " ".join(request.graph_facts),
            request.persona_summary,
            " ".join(request.target.tags),
        )
        intimacy_threshold = 0.72 + (0.05 if slow_burn else 0.0) + max(0.0, state.risk_score - 0.25) * 0.18
        progression_threshold = 0.68 + (0.04 if slow_burn else 0.0)
        return (
            state.intimacy_score >= intimacy_threshold
            and state.progression_score >= progression_threshold
            and state.wechat_ready_score >= 0.68
            and state.risk_score < 0.42
            and positive_ratio >= 0.74
            and tone_warmth >= 0.64
            and (round_count >= 10 or stable_days >= 2)
        )

    def _graph_affinity_score(self, graph_facts: list[str]) -> float:
        text = " ".join(graph_facts or [])
        if not text.strip():
            return 0.0
        negative_markers = ("拒绝", "冷淡", "回避", "抗拒", "slow", "cold")
        positive_markers = ("shared", "prefers", "interest", "舒适", "安全感", "共同", "愿意")
        score = 0.0
        if any(marker in text for marker in positive_markers):
            score += 0.55
        if any(marker in text for marker in negative_markers):
            score -= 0.55
        return self._clamp(0.5 + score) - 0.5

    def _infer_tone_warmth(self, request: RelationshipEvaluateRequest) -> float:
        target_messages = [item.content for item in request.recent_messages if item.role == "target" and item.content]
        return self._tone_warmth_from_messages(target_messages)

    def _infer_program_tone_warmth(self, request: ProgramRunRequest) -> float:
        target_messages = [item.content for item in request.recent_messages if item.role == "target" and item.content]
        return self._tone_warmth_from_messages(target_messages)

    def _tone_warmth_from_messages(self, messages: list[str]) -> float:
        if not messages:
            return 0.35
        positive_markers = ("哈哈", "想", "一起", "可以", "愿意", "期待", "喜欢", "舒服", "试试", "有空")
        cold_markers = ("嗯", "哦", "再说", "忙", "算了", "不想", "没空", "不方便", "看情况")
        score = 0.35
        avg_len = sum(len(item.strip()) for item in messages) / max(len(messages), 1)
        if avg_len >= 12:
            score += 0.12
        elif avg_len >= 7:
            score += 0.06
        for text in messages:
            normalized = text.strip()
            if any(marker in normalized for marker in positive_markers):
                score += 0.08
            if "？" in normalized or "?" in normalized:
                score += 0.04
            if any(marker == normalized for marker in cold_markers):
                score -= 0.12
            elif any(marker in normalized for marker in cold_markers):
                score -= 0.08
        return self._clamp(score)

    def _is_slow_burn_target_text(self, *parts: str) -> bool:
        text = " ".join(part for part in parts if part).strip()
        return any(marker in text for marker in ("慢热", "安全感", "谨慎", "steady", "safe"))

    def _to_float(self, value, fallback: float) -> float:
        try:
            return float(value)
        except Exception:
            return float(fallback)

    def _clamp(self, value: float) -> float:
        return max(0.0, min(1.0, value))
