from agent_runtime.schemas.companion import (
    CompanionGiftPlanRequest,
    CompanionGiftPlanResponse,
    CompanionReplyRequest,
    CompanionReplyResponse,
    CompanionSuggestedAction,
    CompanionStrategyRequest,
    CompanionStrategyResponse,
    GiftPlanItem,
    ReplySuggestion,
)
from agent_runtime.schemas.autonomy import RelationshipEvaluateRequest, RelationshipStateResponse
from agent_runtime.services.companion_action_planning import CompanionActionPlanningService
from agent_runtime.services.intelligence import AgentIntelligenceService
from agent_runtime.services.llm import StructuredLlmService
from agent_runtime.services.relationship_state import RelationshipStateService
from agent_runtime.services.router import SharedConfigRouter
from agent_runtime.services.trace_store import RuntimeTraceService
from agent_runtime.services.tracing import DebugTrace, apply_debug_visibility, measure_step
from agent_runtime.skills.registry import SkillRegistry


class CompanionService:
    def __init__(
        self,
        router: SharedConfigRouter,
        skill_registry: SkillRegistry,
        intelligence: AgentIntelligenceService,
        llm: StructuredLlmService,
        trace_store: RuntimeTraceService,
        action_planning: CompanionActionPlanningService,
        relationship_state: RelationshipStateService,
    ) -> None:
        self.router = router
        self.skill_registry = skill_registry
        self.intelligence = intelligence
        self.llm = llm
        self.trace_store = trace_store
        self.action_planning = action_planning
        self.relationship_state = relationship_state

    def suggest_reply(self, payload: CompanionReplyRequest) -> CompanionReplyResponse:
        scene_code = "companion.reply"
        function_type = "companion_reply"
        route = self.llm.resolve_route(scene_code=scene_code, function_type=function_type)
        trace = DebugTrace(model_provider=route.provider_code, model_profile=route.profile_code or "")
        context = measure_step(trace, "context_build", lambda: self.intelligence.build_companion_context(payload))
        trace.retrieval_hits = context.memory_hits[:4]
        trace.graph_facts = context.graph_facts[:4]
        stage = payload.memory.stage
        topic = context.topic_hint or self._pick_topic(payload)
        fallback_suggestions = [
            ReplySuggestion(
                style="warm",
                text=f"今天看到一个和{topic}有关的小细节，突然想到你，最近怎么样？",
                rationale="先给轻共鸣，再落到开放式问句，适合建立连续对话。",
                risk_level="low",
            ),
            ReplySuggestion(
                style="playful",
                text=f"我怀疑你对{topic}应该挺有研究，不然怎么总能让我想到这个话题。",
                rationale="带一点轻逗趣，适合不太冷的聊天氛围。",
                risk_level="medium",
            ),
            ReplySuggestion(
                style="steady",
                text=f"这两天节奏有点快，想起你之前提过的{topic}，还挺想听你展开聊聊。",
                rationale="适合慢热对象，表达克制，不会显得压迫。",
                risk_level="low",
            ),
        ]
        memory_updates = [
            f"relationship_stage={stage}",
            f"owner_goal={payload.owner_goal}",
            f"topic_anchor={topic}",
        ]
        if context.memory_hits:
            memory_updates.append(f"memory_recall={context.memory_hits[0]}")
        skill_hints = [
            skill.code for skill in self.skill_registry.list_skills()
            if skill.code in payload.available_skills
        ]
        fallback_response = CompanionReplyResponse(
            provider=route.provider_code,
            route_profile=route.profile_code,
            suggestions=fallback_suggestions,
            strategy_note=self._strategy_note(payload, context),
            memory_updates=memory_updates,
            skill_hints=skill_hints,
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary="回复建议基于双方记忆召回、关系阶段和当前目标生成。",
        )
        llm_result = measure_step(
            trace,
            "llm_generate",
            lambda: self.llm.complete_json(
                route=route,
                system_prompt=(
                    "你是丘偶的智能恋爱助手。"
                    "你必须只输出 JSON，不要 markdown，不要解释。"
                    "输出格式必须是 {suggestions:[{style,text,rationale,risk_level}], strategy_note, memory_updates, skill_hints, reasoning_summary}。"
                    "suggestions 至少返回 3 条，中文自然，避免油腻。"
                ),
                user_payload={
                    "owner": payload.owner.model_dump(mode="json"),
                    "target": payload.target.model_dump(mode="json"),
                    "memory": payload.memory.model_dump(mode="json"),
                    "persona_kernel": None if payload.persona_kernel is None else payload.persona_kernel.model_dump(mode="json"),
                    "recent_messages": [item.model_dump(mode="json") for item in payload.recent_messages[-8:]],
                    "owner_goal": payload.owner_goal,
                    "desired_tone": payload.desired_tone,
                    "available_skills": payload.available_skills,
                    "retrieval_hits": trace.retrieval_hits,
                    "retrieval_status": "no_result" if not trace.retrieval_hits else "hit",
                    "graph_facts": trace.graph_facts,
                    "topic_hint": topic,
                },
                fallback=fallback_response.model_dump(mode="json"),
            ),
        )
        suggestions = self._pick_suggestions(llm_result.get("suggestions") if isinstance(llm_result, dict) else None, fallback_suggestions)
        response = CompanionReplyResponse(
            provider=route.provider_code,
            route_profile=route.profile_code,
            suggestions=suggestions,
            strategy_note=self._pick_text(llm_result.get("strategy_note") if isinstance(llm_result, dict) else None, fallback_response.strategy_note),
            memory_updates=self._pick_list(llm_result.get("memory_updates") if isinstance(llm_result, dict) else None, memory_updates, 4),
            skill_hints=self._pick_list(llm_result.get("skill_hints") if isinstance(llm_result, dict) else None, skill_hints, 4),
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary=self._pick_text(llm_result.get("reasoning_summary") if isinstance(llm_result, dict) else None, fallback_response.reasoning_summary or ""),
        )
        trace.reasoning_summary = response.reasoning_summary or ""
        self.intelligence.record_companion_reply(payload, [item.text for item in response.suggestions])
        self.trace_store.record(
            agent_type="companion",
            scene_code=scene_code,
            function_type=function_type,
            trace=trace,
            owner_user_id=payload.owner.user_id if payload.owner else None,
            target_user_id=payload.target.user_id if payload.target else None,
            request_json={
                "owner": payload.owner.model_dump(mode="json"),
                "target": payload.target.model_dump(mode="json"),
                "memory": payload.memory.model_dump(mode="json"),
                "recent_messages": [item.model_dump(mode="json") for item in payload.recent_messages[-8:]],
                "owner_goal": payload.owner_goal,
                "desired_tone": payload.desired_tone,
            },
            response_json=response.model_dump(mode="json"),
            request_summary=f"reply owner={payload.owner.user_id} target={payload.target.user_id} stage={stage}",
            response_summary=response.strategy_note,
        )
        return apply_debug_visibility(response, self.intelligence.settings.llm_enable_debug_trace)

    def next_step(self, payload: CompanionStrategyRequest) -> CompanionStrategyResponse:
        scene_code = "companion.next_step"
        function_type = "companion_next_step"
        route = self.llm.resolve_route(scene_code=scene_code, function_type=function_type)
        trace = DebugTrace(model_provider=route.provider_code, model_profile=route.profile_code or "")
        context = measure_step(trace, "context_build", lambda: self.intelligence.build_companion_context(payload))
        trace.retrieval_hits = context.memory_hits[:4]
        trace.graph_facts = context.graph_facts[:4]
        relationship_state = self.relationship_state.evaluate(
            RelationshipEvaluateRequest(
                owner=payload.owner,
                target=payload.target,
                recent_messages=payload.recent_messages,
                behavior_signals=payload.behavior_signals,
                relationship_tags=payload.target.tags,
                graph_facts=context.graph_facts[:6],
                persona_summary=" ".join(context.memory_hits[:3]),
                persona_kernel=payload.persona_kernel,
            )
        )
        stage = relationship_state.stage_code or self._infer_stage(payload, context)
        next_action = self._next_action(stage, payload)
        plan = self._build_plan(stage, payload, context)
        suggested_actions = self._build_suggested_actions(stage, payload, context)
        planned_actions = self.action_planning.plan(
            owner=payload.owner,
            target=payload.target,
            actions=suggested_actions,
            policy_context=payload.policy_context,
        )
        guardrails = [
            "不要自动发出线下邀约，除非 Java 业务侧再次确认授权。",
            "如果对方最近反馈稀疏，优先降频而不是连续追问。",
            "涉及金钱、联系方式交换、深夜高情绪表达时必须人工确认。",
        ]
        if context.graph_facts:
            guardrails.append("图谱显示关系还在早期阶段时，优先维持自然节奏。")
        fallback_response = CompanionStrategyResponse(
            provider=route.provider_code,
            route_profile=route.profile_code,
            relationship_stage=stage,
            heat_score=relationship_state.heat_score,
            trust_score=relationship_state.trust_score,
            tone_warmth=relationship_state.tone_warmth,
            intimacy_score=relationship_state.intimacy_score,
            progression_score=relationship_state.progression_score,
            date_ready_score=relationship_state.date_ready_score,
            wechat_ready_score=relationship_state.wechat_ready_score,
            risk_score=relationship_state.risk_score,
            next_best_action=next_action,
            next_24h_plan=plan,
            guardrails=guardrails,
            suggested_actions=planned_actions.actions,
            should_auto_execute=planned_actions.allow_auto_execute,
            action_preflight=planned_actions.decisions,
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary="推进策略基于关系阶段、记忆召回与图谱事实生成。",
        )
        llm_result = measure_step(
            trace,
            "llm_generate",
            lambda: self.llm.complete_json(
                route=route,
                system_prompt=(
                    "你是丘偶的智能恋爱策略助手。"
                    "你必须只输出 JSON，不要 markdown，不要解释。"
                    "输出格式必须是 {relationship_stage,heat_score,trust_score,tone_warmth,intimacy_score,progression_score,date_ready_score,wechat_ready_score,risk_score,next_best_action,next_24h_plan,guardrails,suggested_actions,should_auto_execute,reasoning_summary}。"
                    "suggested_actions 每项必须包含 action_type, capability_code, title, summary, payload, risk_level, execute_mode, requires_approval。"
                    "action_type 应优先从平台动作目录中选择。"
                    "如果没有明确把握，就返回 message_auto_send 或 gift_plan。"
                    "should_auto_execute 只表达策略倾向，最终仍由策略门决定。"
                ),
                user_payload={
                    "owner": payload.owner.model_dump(mode="json"),
                    "target": payload.target.model_dump(mode="json"),
                    "memory": payload.memory.model_dump(mode="json"),
                    "persona_kernel": None if payload.persona_kernel is None else payload.persona_kernel.model_dump(mode="json"),
                    "recent_messages": [item.model_dump(mode="json") for item in payload.recent_messages[-8:]],
                    "behavior_signals": [item.model_dump(mode="json") for item in payload.behavior_signals[-8:]],
                    "objective": payload.objective,
                    "policy_context": None if payload.policy_context is None else payload.policy_context.model_dump(mode="json"),
                    "retrieval_hits": trace.retrieval_hits,
                    "retrieval_status": "no_result" if not trace.retrieval_hits else "hit",
                    "graph_facts": trace.graph_facts,
                    "relationship_state": relationship_state.model_dump(mode="json"),
                },
                fallback=fallback_response.model_dump(mode="json"),
            ),
        )
        raw_actions = self._pick_actions(llm_result.get("suggested_actions") if isinstance(llm_result, dict) else None, fallback_response.suggested_actions)
        action_plan = self.action_planning.plan(
            owner=payload.owner,
            target=payload.target,
            actions=raw_actions,
            policy_context=payload.policy_context,
        )
        response = CompanionStrategyResponse(
            provider=route.provider_code,
            route_profile=route.profile_code,
            relationship_stage=self._pick_text(llm_result.get("relationship_stage") if isinstance(llm_result, dict) else None, fallback_response.relationship_stage),
            heat_score=self._pick_score(llm_result.get("heat_score") if isinstance(llm_result, dict) else None, fallback_response.heat_score),
            trust_score=self._pick_score(llm_result.get("trust_score") if isinstance(llm_result, dict) else None, fallback_response.trust_score),
            tone_warmth=self._pick_score(llm_result.get("tone_warmth") if isinstance(llm_result, dict) else None, fallback_response.tone_warmth),
            intimacy_score=self._pick_score(llm_result.get("intimacy_score") if isinstance(llm_result, dict) else None, fallback_response.intimacy_score),
            progression_score=self._pick_score(llm_result.get("progression_score") if isinstance(llm_result, dict) else None, fallback_response.progression_score),
            date_ready_score=self._pick_score(llm_result.get("date_ready_score") if isinstance(llm_result, dict) else None, fallback_response.date_ready_score),
            wechat_ready_score=self._pick_score(llm_result.get("wechat_ready_score") if isinstance(llm_result, dict) else None, fallback_response.wechat_ready_score),
            risk_score=self._pick_score(llm_result.get("risk_score") if isinstance(llm_result, dict) else None, fallback_response.risk_score),
            next_best_action=self._pick_text(llm_result.get("next_best_action") if isinstance(llm_result, dict) else None, fallback_response.next_best_action),
            next_24h_plan=self._pick_list(llm_result.get("next_24h_plan") if isinstance(llm_result, dict) else None, fallback_response.next_24h_plan, 4),
            guardrails=self._pick_list(llm_result.get("guardrails") if isinstance(llm_result, dict) else None, fallback_response.guardrails, 5),
            suggested_actions=action_plan.actions,
            should_auto_execute=action_plan.allow_auto_execute,
            action_preflight=action_plan.decisions,
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary=self._pick_text(llm_result.get("reasoning_summary") if isinstance(llm_result, dict) else None, fallback_response.reasoning_summary or ""),
        )
        trace.reasoning_summary = response.reasoning_summary or ""
        self.intelligence.record_companion_strategy(payload, response.next_best_action, response.next_24h_plan)
        self.trace_store.record(
            agent_type="companion",
            scene_code=scene_code,
            function_type=function_type,
            trace=trace,
            owner_user_id=payload.owner.user_id if payload.owner else None,
            target_user_id=payload.target.user_id if payload.target else None,
            request_json={
                "owner": payload.owner.model_dump(mode="json"),
                "target": payload.target.model_dump(mode="json"),
                "memory": payload.memory.model_dump(mode="json"),
                "recent_messages": [item.model_dump(mode="json") for item in payload.recent_messages[-8:]],
                "behavior_signals": [item.model_dump(mode="json") for item in payload.behavior_signals[-8:]],
                "objective": payload.objective,
            },
            response_json=response.model_dump(mode="json"),
            request_summary=f"strategy owner={payload.owner.user_id} target={payload.target.user_id} objective={payload.objective}",
            response_summary=response.next_best_action,
        )
        return apply_debug_visibility(response, self.intelligence.settings.llm_enable_debug_trace)

    def plan_gift(self, payload: CompanionGiftPlanRequest) -> CompanionGiftPlanResponse:
        route = self.router.resolve_ai_route(scene_code="companion.gift_plan", function_type="companion_gift_plan")
        context = self.intelligence.build_companion_context(payload)
        stage = self._infer_gift_stage(payload, context)
        topic = self._pick_gift_topic(payload, context)
        city = payload.gift_context.target_city or payload.target.city or "同城"
        budgets = self._normalize_budgets(payload.budget_options)
        templates = [
            ("milk_tea", "爱你的第一杯奶茶", "先把气氛变甜一点", "微糖晚风", "🧋", "tea"),
            ("starlight", "想你的星光", "把今晚的好感点亮", "夜空发亮", "✨", "star"),
            ("heartbeat", "超想见你", "想把期待直接递给你", "心跳上线", "💌", "heart"),
            ("goodnight", "偏爱到底", "认真这件事不想收回", "月色偏爱", "🌙", "night"),
        ]
        gifts: list[GiftPlanItem] = []
        owner_name = payload.owner.nickname or "我"
        target_name = payload.target.nickname or "你"
        for index, template in enumerate(templates):
            code, name, desc, scene, icon, theme = template
            amount = budgets[min(index, len(budgets) - 1)]
            gifts.append(
                GiftPlanItem(
                    code=code,
                    name=name,
                    desc=desc,
                    scene=scene,
                    icon=icon,
                    theme=theme,
                    amount=amount,
                    display_amount=self._format_amount(amount),
                    message_draft=self._gift_message(stage, name, topic, city, target_name),
                    rationale=self._gift_rationale(stage, name, topic),
                    visual_prompt=(
                        f"{owner_name}在{city}的真实生活场景里，把{name}送给{target_name}，"
                        f"氛围围绕{topic}展开，东亚年轻人，电影感生活照。"
                    ),
                    motion_prompt=(
                        f"{owner_name}走向{target_name}并送出{name}，场景在{city}，"
                        f"围绕{topic}展开，镜头自然推进，突出真诚互动。"
                    ),
                    next_action="open_chat",
                    risk_level="low",
                    recommended=index == 0,
                )
            )
        strategy_note = self._gift_strategy_note(stage, topic)
        wechat_prompt = self._wechat_prompt(stage, topic, target_name)
        response = CompanionGiftPlanResponse(
            provider=route.provider_code,
            route_profile=route.profile_code,
            scene=payload.scene,
            relationship_stage=stage,
            strategy_note=strategy_note,
            wechat_prompt=wechat_prompt,
            gifts=gifts,
        )
        self.intelligence.record_companion_gift_plan(payload, [gift.message_draft for gift in gifts], strategy_note)
        return response

    def _pick_topic(self, payload: CompanionReplyRequest) -> str:
        if payload.target.tags:
            return payload.target.tags[0]
        if payload.memory.known_preferences:
            return payload.memory.known_preferences[0]
        return "最近的生活"

    def _strategy_note(self, payload: CompanionReplyRequest, context) -> str:
        if payload.memory.stage == "early":
            note = "当前更适合让对方觉得轻松和被理解，而不是直接推进关系。"
            if context.memory_hits:
                return f"{note} 记忆里能承接的话题是：{context.memory_hits[0]}。"
            return note
        if payload.memory.stage == "warming":
            return "可以适度增加个人表达，但仍要围绕对方反馈做节奏控制。"
        return "关系进入稳定区后，重点是持续感和兑现感。"

    def _infer_stage(self, payload: CompanionStrategyRequest, context) -> str:
        if payload.memory.stage:
            return payload.memory.stage
        if context.graph_facts:
            return "warming"
        if len(payload.recent_messages) >= 12:
            return "warming"
        return "early"

    def _next_action(self, stage: str, payload: CompanionStrategyRequest) -> str:
        if stage == "early":
            return "发送一条低压力、可自然接续的话题消息。"
        if stage == "warming":
            return "围绕共同兴趣做一次更具体的互动提议，但不直接线下邀约。"
        return "通过稳定陪伴感和兑现细节来提升信任。"

    def _build_plan(self, stage: str, payload: CompanionStrategyRequest, context) -> list[str]:
        if stage == "early":
            plan = [
                "先从对方最近标签或动态里选一个轻话题。",
                "等待反馈后再追加一个个人化回应。",
                "如果 6-12 小时内没有回应，不连续追发。",
            ]
            if context.memory_hits:
                plan[0] = f"优先从记忆召回的话题切入，比如：{context.memory_hits[0]}。"
            return plan
        if stage == "warming":
            plan = [
                "先延续最近一次聊得顺的主题。",
                "再给一个可选式互动提议，比如一起看同类内容。",
                "根据反馈决定是否升级到更直接的关系表达。",
            ]
            if context.graph_facts:
                plan.append(f"参考图谱事实：{context.graph_facts[0]}")
            return plan[:3]
        return [
            "围绕既有默契做一次更具体的陪伴式表达。",
            "避免模板化夸赞，增加回忆型细节。",
            "涉及高风险动作先暂停自动执行。",
        ]

    def _build_suggested_actions(self, stage: str, payload: CompanionStrategyRequest, context) -> list[CompanionSuggestedAction]:
        topic = context.topic_hint or self._infer_topic_from_strategy(payload)
        message_text = self._build_message_action_text(stage, topic, payload)
        actions = [
            CompanionSuggestedAction(
                action_type="message_auto_send",
                capability_code="message.auto.send",
                title="发一条轻压力消息",
                summary="优先延续自然话题，把聊天重新接起来。",
                payload={
                    "content": message_text,
                    "source": "next_step",
                },
                risk_level="low",
                execute_mode="auto_if_permitted",
                requires_approval=False,
            ),
            CompanionSuggestedAction(
                action_type="gift_plan",
                capability_code="gift.plan",
                title="先看礼物策划",
                summary="如果聊天反馈顺畅，再决定是否用轻量礼物推进关系。",
                payload={
                    "objective": "break_ice" if stage == "early" else "express_interest",
                    "scene": "social_intent",
                },
                risk_level="low",
                execute_mode="draft_only",
                requires_approval=False,
            ),
        ]
        if stage == "warming":
            actions[0].title = "发一条更具体的互动消息"
            actions[0].summary = "关系升温阶段适合发一个更具体但不压迫的互动提议。"
            actions[0].payload["content"] = self._build_message_action_text(stage, topic, payload)
            actions[0].payload["source"] = "next_step_warming"
        return actions

    def _infer_gift_stage(self, payload: CompanionGiftPlanRequest, context) -> str:
        if payload.memory.stage:
            return payload.memory.stage
        if context.graph_facts or len(payload.recent_messages) >= 8:
            return "warming"
        return "early"

    def _pick_gift_topic(self, payload: CompanionGiftPlanRequest, context) -> str:
        if payload.gift_context.target_interest:
            return payload.gift_context.target_interest
        if payload.target.tags:
            return payload.target.tags[0]
        if payload.memory.known_preferences:
            return payload.memory.known_preferences[0]
        if context.topic_hint:
            return context.topic_hint
        return "最近的生活"

    def _normalize_budgets(self, budgets: list[int]) -> list[int]:
        cleaned = [item for item in budgets if isinstance(item, int) and 100 <= item <= 999999]
        return cleaned or [1314, 52100, 66600, 168800]

    def _format_amount(self, amount: int) -> str:
        text = f"{amount / 100:.2f}"
        return text.rstrip("0").rstrip(".")

    def _gift_message(self, stage: str, gift_name: str, topic: str, city: str, target_name: str) -> str:
        if stage == "warming":
            return f"看到你最近聊到{topic}，想把这份{gift_name}送给{target_name}，认真回应一下这份好感。"
        return f"刷到你时一下记住了{city}和{topic}这点，先把这份{gift_name}送给你，想自然地认识你。"

    def _gift_rationale(self, stage: str, gift_name: str, topic: str) -> str:
        if stage == "warming":
            return f"现在适合用{gift_name}把回应感拉高，但还不用过度推进。"
        return f"先用轻量礼物承接{topic}这个话题，会比直接硬聊更自然。"

    def _gift_strategy_note(self, stage: str, topic: str) -> str:
        if stage == "warming":
            return f"当前更适合送一份有记忆点但不过分夸张的礼物，把“我记得你在意{topic}”这件事传递出来。"
        return "当前关系还在早期，礼物应该轻一点、具体一点，重点是让对方感受到你有观察和诚意，而不是单纯砸金额。"

    def _wechat_prompt(self, stage: str, topic: str, target_name: str) -> str:
        if stage == "warming":
            return f"和{target_name}聊到{topic}这件事时，可以顺势说一句：如果你也觉得聊得舒服，我们再交换微信。"
        return f"先围绕{topic}把聊天接起来，等{target_name}连续有回应后，再自然申请微信会更顺。"

    def _pick_suggestions(self, value, fallback: list[ReplySuggestion]) -> list[ReplySuggestion]:
        suggestions: list[ReplySuggestion] = []
        if isinstance(value, list):
            for item in value[:3]:
                if not isinstance(item, dict):
                    continue
                text = self._pick_text(item.get("text"), "")
                if not text:
                    continue
                suggestions.append(
                    ReplySuggestion(
                        style=self._pick_text(item.get("style"), "warm"),
                        text=text,
                        rationale=self._pick_text(item.get("rationale"), "结合当前关系阶段与最近话题生成。"),
                        risk_level=self._pick_text(item.get("risk_level"), "low"),
                    )
                )
        return suggestions or fallback

    def _pick_text(self, value, fallback: str) -> str:
        text = str(value or "").strip()
        return text or fallback

    def _pick_list(self, value, fallback: list[str], limit: int) -> list[str]:
        if isinstance(value, list):
            normalized = [str(item).strip() for item in value if str(item).strip()]
            if normalized:
                return normalized[:limit]
        return fallback[:limit]

    def _pick_score(self, value, fallback: float) -> float:
        try:
            score = float(value)
        except (TypeError, ValueError):
            return fallback
        return max(0.0, min(1.0, round(score, 4)))

    def _pick_actions(self, value, fallback: list[CompanionSuggestedAction]) -> list[CompanionSuggestedAction]:
        actions: list[CompanionSuggestedAction] = []
        if isinstance(value, list):
            for item in value[:3]:
                if not isinstance(item, dict):
                    continue
                action_type = self._pick_text(item.get("action_type"), "")
                if action_type not in self.action_planning.action_registry.allowed_action_types():
                    continue
                actions.append(
                    CompanionSuggestedAction(
                        action_type=action_type,
                        capability_code=self._pick_text(
                            item.get("capability_code"),
                            "message.auto.send" if action_type == "message_auto_send" else "gift.plan",
                        ),
                        title=self._pick_text(item.get("title"), "动作建议"),
                        summary=self._pick_text(item.get("summary"), "建议先人工确认后再执行。"),
                        payload=item.get("payload") if isinstance(item.get("payload"), dict) else {},
                        risk_level=self._pick_text(item.get("risk_level"), "low"),
                        execute_mode=self._pick_text(item.get("execute_mode"), "draft_only"),
                        requires_approval=bool(item.get("requires_approval")),
                    )
                )
        return actions or fallback

    def _infer_topic_from_strategy(self, payload: CompanionStrategyRequest) -> str:
        if payload.target.tags:
            return payload.target.tags[0]
        if payload.memory.known_preferences:
            return payload.memory.known_preferences[0]
        return "最近的生活"

    def _build_message_action_text(self, stage: str, topic: str, payload: CompanionStrategyRequest) -> str:
        target_name = payload.target.nickname or "你"
        if stage == "warming":
            return f"上次聊到{topic}我还记着，感觉和{target_name}继续展开这个话题会很舒服，你这两天怎么样？"
        return f"刚好想到你之前提过的{topic}，突然有点想继续和你聊聊，最近过得怎么样？"
