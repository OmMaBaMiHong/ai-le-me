from __future__ import annotations

import ast

from agent_runtime.schemas.persona import CoachStyleCandidate, PersonaKernel
from agent_runtime.schemas.common import TraitScore
from agent_runtime.schemas.persona import PersonaReportRequest, PersonaReportResponse
from agent_runtime.services.intelligence import AgentIntelligenceService
from agent_runtime.services.llm import StructuredLlmService
from agent_runtime.services.trace_store import RuntimeTraceService
from agent_runtime.services.tracing import DebugTrace, apply_debug_visibility, measure_step
from agent_runtime.services.coach_style import CoachStyleService


class PersonaReportService:
    def __init__(
        self,
        llm: StructuredLlmService,
        intelligence: AgentIntelligenceService,
        trace_store: RuntimeTraceService,
    ) -> None:
        self.llm = llm
        self.intelligence = intelligence
        self.trace_store = trace_store

    def generate(self, payload: PersonaReportRequest) -> PersonaReportResponse:
        scene_code = "persona.report"
        function_type = "persona_report"
        route = self.llm.resolve_route(scene_code=scene_code, function_type=function_type)
        trace = DebugTrace(model_provider=route.provider_code, model_profile=route.profile_code or "")
        context = measure_step(trace, "context_build", lambda: self.intelligence.build_persona_context(payload))
        trace.retrieval_hits = context.memory_hits[:4]
        trace.graph_facts = context.graph_facts[:4]

        fallback = self._build_fallback_response(payload, route.provider_code, route.profile_code, trace)
        llm_payload = {
            "owner": payload.owner.model_dump(mode="json") if payload.owner else None,
            "target": payload.target.model_dump(mode="json"),
            "recent_messages": [item.model_dump(mode="json") for item in payload.recent_messages[-8:]],
            "relationship_tags": payload.relationship_tags,
            "graph_facts": payload.graph_facts,
            "recent_moments": [item.model_dump(mode="json") for item in payload.recent_moments[-6:]],
            "behavior_signals": [item.model_dump(mode="json") for item in payload.behavior_signals[-8:]],
            "memory_hits": context.memory_hits[:4],
            "retrieval_status": "no_result" if not context.memory_hits else "hit",
            "graph_enrichment": context.graph_facts[:4],
            "interest_hints": context.interest_hints[:6],
            "report_goal": payload.report_goal or "dating_companion",
        }

        llm_result = measure_step(
            trace,
            "llm_generate",
            lambda: self.llm.complete_json(
                route=route,
                system_prompt=(
                    "你是丘偶的智能画像 Agent。"
                    "你必须只输出 JSON，不要 markdown，不要解释。"
                    "输出字段必须包含 summary, core_traits, emotional_style, attachment_style, "
                    "interest_clusters, risk_flags, approach_suggestions, evidence_digest, reasoning_summary, persona_kernel。"
                    "core_traits 必须是长度 3 的数组，每项包含 name, score, reason。"
                    "persona_kernel 必须包含 stable_traits, expression_style, relationship_style, attachment_style, "
                    "love_language, romance_pace, taboo_rules, coach_style_candidates, preferred_master_style_code, style_router_reason。"
                    "coach_style_candidates 最多 3 个，每项包含 style_code, style_name, fit_score, reason。"
                    "内容要贴近中文社交产品语境，避免空话。"
                ),
                user_payload=llm_payload,
                fallback=self._response_to_dict(fallback),
            ),
        )

        response = self._build_response_from_result(llm_result or {}, fallback, route.provider_code, route.profile_code, trace)
        self.intelligence.record_persona(payload, response)
        self.trace_store.record(
            agent_type="persona",
            scene_code=scene_code,
            function_type=function_type,
            trace=trace,
            owner_user_id=payload.owner.user_id if payload.owner else None,
            target_user_id=payload.target.user_id if payload.target else None,
            request_json=llm_payload,
            response_json=response.model_dump(mode="json"),
            request_summary=f"persona target={payload.target.user_id} messages={len(payload.recent_messages)} signals={len(payload.behavior_signals)}",
            response_summary=response.summary,
        )
        return apply_debug_visibility(response, self.intelligence.settings.llm_enable_debug_trace)

    def _build_response_from_result(
        self,
        result: dict,
        fallback: PersonaReportResponse,
        provider: str,
        profile: str | None,
        trace: DebugTrace,
    ) -> PersonaReportResponse:
        traits = result.get("core_traits") or []
        normalized_traits: list[TraitScore] = []
        for item in traits[:3]:
            if not isinstance(item, dict):
                continue
            try:
                normalized_traits.append(
                    TraitScore(
                        name=str(item.get("name") or "trait"),
                        score=max(0, min(100, int(item.get("score") or 60))),
                        reason=str(item.get("reason") or "基于资料与互动信号综合判断。"),
                    )
                )
            except Exception:
                continue
        if len(normalized_traits) < 3:
            normalized_traits = fallback.core_traits

        fallback_kernel = fallback.persona_kernel
        result_kernel = result.get("persona_kernel") if isinstance(result.get("persona_kernel"), dict) else {}
        persona_kernel = self._build_persona_kernel(result_kernel, fallback_kernel)

        response = PersonaReportResponse(
            provider=provider,
            route_profile=profile,
            summary=self._pick_text(result.get("summary"), fallback.summary),
            core_traits=normalized_traits,
            emotional_style=self._pick_text_like(result.get("emotional_style"), fallback.emotional_style),
            attachment_style=persona_kernel.attachment_style,
            interest_clusters=self._pick_list(result.get("interest_clusters"), fallback.interest_clusters, 6),
            risk_flags=self._pick_list(result.get("risk_flags"), fallback.risk_flags, 4),
            approach_suggestions=self._pick_list(result.get("approach_suggestions"), fallback.approach_suggestions, 4),
            evidence_digest=self._pick_list(result.get("evidence_digest"), fallback.evidence_digest, 5),
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary=self._pick_text(result.get("reasoning_summary"), "画像基于资料、记忆召回与关系图谱综合生成。"),
            persona_kernel=persona_kernel,
        )
        trace.reasoning_summary = response.reasoning_summary or ""
        return response

    def _build_fallback_response(
        self,
        payload: PersonaReportRequest,
        provider: str,
        profile: str | None,
        trace: DebugTrace,
    ) -> PersonaReportResponse:
        message_count = len(payload.recent_messages)
        moment_count = len(payload.recent_moments)
        all_target_tags = self._merge_unique(payload.target.tags, trace.retrieval_hits)
        all_relationship_tags = self._merge_unique(payload.relationship_tags, trace.graph_facts)
        tag_count = len(all_target_tags) + len(all_relationship_tags)

        stability = 45 + min(message_count * 3, 25) + min(len(trace.graph_facts) * 2, 6)
        openness = 40 + min(tag_count * 4, 30)
        engagement = 35 + min((message_count + moment_count) * 4, 35) + min(len(trace.retrieval_hits) * 2, 8)

        evidence = self._build_evidence(payload, trace)
        interests = self._derive_interests(payload, trace)
        risk_flags = self._derive_risks(payload)
        suggestions = self._derive_suggestions(risk_flags, interests)
        persona_kernel = self._build_fallback_persona_kernel(payload, trace)

        return PersonaReportResponse(
            provider=provider,
            route_profile=profile,
            summary=self._build_summary(payload, message_count, moment_count),
            core_traits=[
                TraitScore(name="stability", score=min(stability, 90), reason="Recent interactions look relatively steady."),
                TraitScore(name="openness", score=min(openness, 90), reason="Tags and topics show willingness to engage."),
                TraitScore(name="engagement", score=min(engagement, 90), reason="Chat and moments provide active social signals."),
            ],
            emotional_style=self._derive_emotional_style(payload, message_count),
            attachment_style=self._derive_attachment_style(payload, message_count),
            interest_clusters=interests,
            risk_flags=risk_flags,
            approach_suggestions=suggestions,
            evidence_digest=evidence,
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary="画像基于资料、历史互动、向量记忆与图谱补充信号生成。",
            persona_kernel=persona_kernel,
        )

    def _response_to_dict(self, response: PersonaReportResponse) -> dict:
        return response.model_dump(mode="json")

    def _build_summary(self, payload: PersonaReportRequest, message_count: int, moment_count: int) -> str:
        nickname = payload.target.nickname or "对方"
        city = payload.target.city or "未知城市"
        if message_count >= 8:
            return f"{nickname}在{city}的互动信号比较稳定，适合走真诚、慢推进、带轻松分享感的关系节奏。"
        if moment_count >= 3:
            return f"{nickname}公开表达欲不低，适合从动态共鸣切入，再逐渐转向更私密的聊天。"
        return f"{nickname}当前有效样本还不算多，建议先通过轻话题建立安全感，再逐步收集更深层偏好。"

    def _derive_emotional_style(self, payload: PersonaReportRequest, message_count: int) -> str:
        if any("慢热" in tag for tag in payload.target.tags + payload.relationship_tags):
            return "慢热、需要安全感，回应后再逐步打开。"
        if message_count >= 8:
            return "情绪表达相对稳定，能接住真诚表达，但不宜过快推进。"
        return "当前更适合轻量互动和观察反馈，避免一次性输出过满。"

    def _derive_attachment_style(self, payload: PersonaReportRequest, message_count: int) -> str:
        if any("缺安全感" in fact for fact in payload.graph_facts):
            return "偏敏感依恋，需要稳定回应和可预期表达。"
        if message_count >= 10:
            return "呈现出较稳的连接倾向，适合建立固定互动节奏。"
        return "依恋模式信息不足，先用低压互动建立样本。"

    def _derive_interests(self, payload: PersonaReportRequest, trace: DebugTrace) -> list[str]:
        interests: list[str] = []
        for source in (payload.target.tags, payload.relationship_tags):
            for item in source:
                text = item.strip()
                if text and text not in interests:
                    interests.append(text)
        for moment in payload.recent_moments:
            for tag in moment.tags:
                cleaned = tag.strip()
                if cleaned and cleaned not in interests:
                    interests.append(cleaned)
        for hint in trace.retrieval_hits + trace.graph_facts:
            cleaned = hint.strip()
            if cleaned and cleaned not in interests:
                interests.append(cleaned)
        return interests[:6] or ["轻松聊天", "生活分享"]

    def _derive_risks(self, payload: PersonaReportRequest) -> list[str]:
        risks: list[str] = []
        if len(payload.recent_messages) <= 2:
            risks.append("样本偏少，过度解读风险较高")
        if any("前任" in message.content for message in payload.recent_messages):
            risks.append("近期对话涉及情感旧账，推进节奏要放缓")
        if any("忙" in message.content for message in payload.recent_messages):
            risks.append("对方可能处于高占用状态，频率不宜过密")
        return risks[:3] or ["当前样本仍偏少，建议持续观察反馈。"]

    def _derive_suggestions(self, risk_flags: list[str], interests: list[str]) -> list[str]:
        suggestions = [
            "先用共鸣型话题开场，再给一个容易回应的小问题。",
            "优先围绕对方真实标签和最近动态展开，不要上来就强情绪输出。",
            "每轮对话只推进半步，给对方留回应空间。",
        ]
        if interests:
            suggestions[0] = f"优先从“{interests[0]}”相关话题切入，更自然。"
        if risk_flags:
            suggestions.append("当前存在节奏风险，先稳住互动频率，再考虑邀约。")
        return suggestions[:4]

    def _build_evidence(self, payload: PersonaReportRequest, trace: DebugTrace) -> list[str]:
        evidence: list[str] = []
        if payload.target.tags:
            evidence.append(f"目标标签: {', '.join(payload.target.tags[:4])}")
        if payload.relationship_tags:
            evidence.append(f"关系标签: {', '.join(payload.relationship_tags[:4])}")
        if payload.recent_messages:
            evidence.append(f"最近消息样本数: {len(payload.recent_messages)}")
        if payload.recent_moments:
            evidence.append(f"最近动态样本数: {len(payload.recent_moments)}")
        if payload.graph_facts:
            evidence.append(f"关系网事实: {payload.graph_facts[0]}")
        if trace.graph_facts:
            evidence.append(f"图谱补充: {trace.graph_facts[0]}")
        if trace.retrieval_hits:
            evidence.append(f"记忆召回: {trace.retrieval_hits[0]}")
        return evidence[:5]

    def _build_fallback_persona_kernel(self, payload: PersonaReportRequest, trace: DebugTrace) -> PersonaKernel:
        stable_traits = self._build_stable_traits(payload, trace)
        expression_style = self._derive_expression_style(payload)
        relationship_style = self._derive_relationship_style(payload)
        attachment_style = self._derive_attachment_style(payload, len(payload.recent_messages))
        love_language = self._derive_love_language(payload)
        romance_pace = self._derive_romance_pace(payload)
        taboo_rules = self._derive_taboo_rules(payload)
        coach_style_candidates = self._derive_coach_style_candidates(payload, trace, relationship_style, romance_pace)
        preferred_master_style_code = coach_style_candidates[0].style_code if coach_style_candidates else "steady_partner"
        style_router_reason = self._build_style_router_reason(
            payload,
            relationship_style,
            romance_pace,
            preferred_master_style_code,
        )
        return PersonaKernel(
            stable_traits=stable_traits,
            expression_style=expression_style,
            relationship_style=relationship_style,
            attachment_style=attachment_style,
            love_language=love_language,
            romance_pace=romance_pace,
            taboo_rules=taboo_rules,
            coach_style_candidates=coach_style_candidates,
            preferred_master_style_code=preferred_master_style_code,
            style_router_reason=style_router_reason,
        )

    def _build_persona_kernel(self, result: dict, fallback: PersonaKernel) -> PersonaKernel:
        stable_traits = self._pick_trait_scores(result.get("stable_traits"), fallback.stable_traits)
        coach_style_candidates = self._pick_style_candidates(result.get("coach_style_candidates"), fallback.coach_style_candidates)
        preferred_master_style_code = self._pick_text(
            result.get("preferred_master_style_code"),
            fallback.preferred_master_style_code or (coach_style_candidates[0].style_code if coach_style_candidates else "steady_partner"),
        )
        if coach_style_candidates and preferred_master_style_code not in [item.style_code for item in coach_style_candidates]:
            preferred_master_style_code = coach_style_candidates[0].style_code
        return PersonaKernel(
            stable_traits=stable_traits,
            expression_style=self._pick_text_like(result.get("expression_style"), fallback.expression_style),
            relationship_style=self._pick_text_like(result.get("relationship_style"), fallback.relationship_style),
            attachment_style=self._pick_text_like(result.get("attachment_style"), fallback.attachment_style),
            love_language=self._pick_list(result.get("love_language"), fallback.love_language, 4),
            romance_pace=self._pick_text_like(result.get("romance_pace"), fallback.romance_pace),
            taboo_rules=self._pick_list(result.get("taboo_rules"), fallback.taboo_rules, 4),
            coach_style_candidates=coach_style_candidates,
            preferred_master_style_code=preferred_master_style_code,
            style_router_reason=self._pick_text_like(result.get("style_router_reason"), fallback.style_router_reason or ""),
        )

    def _build_stable_traits(self, payload: PersonaReportRequest, trace: DebugTrace) -> list[TraitScore]:
        raw_tags = self._merge_unique(payload.target.tags, payload.relationship_tags, trace.graph_facts)
        candidates: list[TraitScore] = []
        if any("慢热" in item for item in raw_tags):
            candidates.append(TraitScore(name="slow_warm", score=84, reason="对外部推进偏谨慎，先建立安全感再逐步打开。"))
        if any("安全感" in item for item in raw_tags):
            candidates.append(TraitScore(name="safety_need", score=82, reason="更吃稳定回应、真实兑现和低压沟通。"))
        if any(token in item for item in raw_tags for token in ["同城", "散步", "生活", "咖啡", "看展"]):
            candidates.append(TraitScore(name="life_oriented", score=76, reason="对生活感场景更容易产生回应，适合日常化推进。"))
        if len(payload.recent_messages) >= 6:
            candidates.append(TraitScore(name="response_stability", score=72, reason="已有一定对话样本，可观察到相对稳定的互动承接。"))
        return candidates[:4] or [
            TraitScore(name="observed_caution", score=70, reason="当前样本显示对关系推进更偏谨慎和观察。"),
            TraitScore(name="needs_authenticity", score=72, reason="更容易被真实、自然的表达方式承接。"),
        ]

    def _derive_expression_style(self, payload: PersonaReportRequest) -> str:
        if any("慢热" in tag or "安全感" in tag for tag in payload.target.tags + payload.relationship_tags):
            return "偏短句和生活感表达，先接住情绪，再顺着日常慢慢展开。"
        if len(payload.recent_messages) >= 8:
            return "表达不算冷，但更喜欢自然来回，不吃过度包装的话术。"
        return "当前样本偏少，建议先用轻量、具体、可回应的表达收集更多样本。"

    def _derive_relationship_style(self, payload: PersonaReportRequest) -> str:
        combined = " ".join(self._merge_unique(payload.target.tags, payload.relationship_tags, payload.graph_facts))
        if any(token in combined for token in ["慢热", "安全感", "steady", "safe"]):
            return "先建立稳定回应与安全感，再逐步进入更明确的关系推进。"
        if any(token in combined for token in ["同城", "散步", "生活", "咖啡", "看展"]):
            return "更适合用生活感、同城感、共同兴趣做关系升温。"
        return "以真诚、轻松、不给压力的方式持续加深熟悉度。"

    def _derive_love_language(self, payload: PersonaReportRequest) -> list[str]:
        merged = " ".join(self._merge_unique(payload.target.tags, payload.relationship_tags, payload.graph_facts))
        languages: list[str] = []
        if any(token in merged for token in ["安全感", "steady", "safe"]):
            languages.append("稳定回应")
        if any(token in merged for token in ["散步", "看展", "咖啡", "同城", "生活"]):
            languages.append("陪伴相处")
        if any("认真" in token for token in payload.relationship_tags + payload.target.tags):
            languages.append("真诚确认")
        return languages[:3] or ["稳定回应", "真诚确认"]

    def _derive_romance_pace(self, payload: PersonaReportRequest) -> str:
        if any("慢热" in tag or "安全感" in tag for tag in payload.target.tags + payload.relationship_tags):
            return "慢热型，需要稳定承接后再推进邀约、微信或更强情绪表达。"
        if len(payload.recent_messages) >= 10:
            return "中速推进，可以在自然互动中逐步增加关系信号。"
        return "观察型节奏，先确认承接度再决定推进强度。"

    def _derive_taboo_rules(self, payload: PersonaReportRequest) -> list[str]:
        taboo_rules: list[str] = []
        if any("慢热" in tag or "安全感" in tag for tag in payload.target.tags + payload.relationship_tags):
            taboo_rules.extend(
                [
                    "不要刚建立聊天就高压邀约或连续追问关系定义。",
                    "不要用过满的暧昧话术替代真实交流。",
                ]
            )
        if any("忙" in message.content for message in payload.recent_messages):
            taboo_rules.append("对方忙的时候不要高频连发，避免形成压迫感。")
        if not taboo_rules:
            taboo_rules.append("不要脱离真实标签强行制造情绪浓度，先保证自然承接。")
        return taboo_rules[:4]

    def _derive_coach_style_candidates(
        self,
        payload: PersonaReportRequest,
        trace: DebugTrace,
        relationship_style: str,
        romance_pace: str,
    ) -> list[CoachStyleCandidate]:
        summary_parts = [
            payload.target.nickname or "",
            " ".join(payload.target.tags),
            " ".join(payload.relationship_tags),
            " ".join(payload.graph_facts),
            " ".join(trace.graph_facts),
        ]
        summary = " ".join(part.strip() for part in summary_parts if str(part).strip()).lower()
        style_scores = {
            "warm_guardian": 0.36,
            "steady_partner": 0.34,
            "life_companion": 0.32,
            "playful_tease": 0.2,
            "invitation_driver": 0.18,
        }
        if any(token in summary for token in ("慢热", "安全感", "safe", "steady")):
            style_scores["warm_guardian"] += 0.34
            style_scores["steady_partner"] += 0.18
            style_scores["playful_tease"] -= 0.08
        if any(token in summary for token in ("同城", "生活", "散步", "咖啡", "看展")):
            style_scores["life_companion"] += 0.28
        if len(payload.recent_messages) >= 8:
            style_scores["steady_partner"] += 0.08
            style_scores["invitation_driver"] += 0.04
        if "慢热" in romance_pace or "安全感" in relationship_style:
            style_scores["invitation_driver"] -= 0.05

        ranked = sorted(style_scores.items(), key=lambda item: item[1], reverse=True)[:3]
        candidates: list[CoachStyleCandidate] = []
        for style_code, score in ranked:
            meta = CoachStyleService.STYLE_META.get(style_code, {})
            reason = f"{relationship_style} 当前节奏判断为：{romance_pace}"
            candidates.append(
                CoachStyleCandidate(
                    style_code=style_code,
                    style_name=str(meta.get("style_name") or style_code),
                    fit_score=max(0.0, min(1.0, round(score, 2))),
                    reason=reason,
                )
            )
        return candidates

    def _build_style_router_reason(
        self,
        payload: PersonaReportRequest,
        relationship_style: str,
        romance_pace: str,
        preferred_master_style_code: str,
    ) -> str:
        nickname = payload.target.nickname or "对方"
        return (
            f"{nickname}当前更适合“{preferred_master_style_code}”风格。"
            f"原因是关系偏向：{relationship_style}；推进节奏判断为：{romance_pace}"
        )

    def _pick_trait_scores(self, value, fallback: list[TraitScore]) -> list[TraitScore]:
        traits: list[TraitScore] = []
        if isinstance(value, list):
            for item in value[:4]:
                if not isinstance(item, dict):
                    continue
                try:
                    traits.append(
                        TraitScore(
                            name=str(item.get("name") or "trait"),
                            score=max(0, min(100, int(item.get("score") or 60))),
                            reason=str(item.get("reason") or "基于资料与互动信号综合判断。"),
                        )
                    )
                except Exception:
                    continue
        return traits or fallback

    def _pick_style_candidates(self, value, fallback: list[CoachStyleCandidate]) -> list[CoachStyleCandidate]:
        candidates: list[CoachStyleCandidate] = []
        if isinstance(value, list):
            for item in value[:3]:
                if not isinstance(item, dict):
                    continue
                try:
                    candidates.append(
                        CoachStyleCandidate(
                            style_code=str(item.get("style_code") or "steady_partner"),
                            style_name=str(item.get("style_name") or item.get("style_code") or "成熟稳重型"),
                            fit_score=max(0.0, min(1.0, float(item.get("fit_score") or 0.5))),
                            reason=str(item.get("reason") or "基于画像风格路由生成。"),
                        )
                    )
                except Exception:
                    continue
        return candidates or fallback

    def _merge_unique(self, *groups: list[str]) -> list[str]:
        merged: list[str] = []
        for group in groups:
            for item in group:
                text = item.strip()
                if text and text not in merged:
                    merged.append(text)
        return merged

    def _pick_text(self, value, fallback: str) -> str:
        text = str(value or "").strip()
        return text or fallback

    def _pick_text_like(self, value, fallback: str) -> str:
        if isinstance(value, str):
            stripped = value.strip()
            if stripped.startswith("{") and stripped.endswith("}"):
                try:
                    parsed = ast.literal_eval(stripped)
                    if isinstance(parsed, dict):
                        return self._pick_text_like(parsed, fallback)
                except Exception:
                    pass
            if stripped.startswith("[") and stripped.endswith("]"):
                try:
                    parsed = ast.literal_eval(stripped)
                    if isinstance(parsed, list):
                        return self._pick_text_like(parsed, fallback)
                except Exception:
                    pass
        if isinstance(value, dict):
            for key in ("inference", "summary", "text", "reason", "name"):
                text = str(value.get(key) or "").strip()
                if text:
                    return text
        if isinstance(value, list):
            normalized = [str(item).strip() for item in value if str(item).strip()]
            if normalized:
                return "；".join(normalized[:3])
        return self._pick_text(value, fallback)

    def _pick_list(self, value, fallback: list[str], limit: int) -> list[str]:
        if isinstance(value, list):
            normalized = [str(item).strip() for item in value if str(item).strip()]
            if normalized:
                return normalized[:limit]
        return fallback[:limit]
