from __future__ import annotations

import re

from agent_runtime.schemas.match import MatchCandidate, SmartMatchItem, SmartMatchRequest, SmartMatchResponse
from agent_runtime.services.intelligence import AgentIntelligenceService
from agent_runtime.services.llm import StructuredLlmService
from agent_runtime.services.router import SharedConfigRouter
from agent_runtime.services.trace_store import RuntimeTraceService
from agent_runtime.services.tracing import DebugTrace, apply_debug_visibility, measure_step


class SmartMatchService:
    def __init__(
        self,
        router: SharedConfigRouter,
        llm: StructuredLlmService,
        intelligence: AgentIntelligenceService,
        trace_store: RuntimeTraceService,
    ) -> None:
        self.router = router
        self.llm = llm
        self.intelligence = intelligence
        self.trace_store = trace_store

    def recommend(self, payload: SmartMatchRequest) -> SmartMatchResponse:
        scene_code = "match.recommend"
        function_type = "smart_match_recommend"
        route = self.llm.resolve_route(scene_code=scene_code, function_type=function_type)
        ranked = [self._rank_candidate(payload, candidate) for candidate in payload.candidates]
        ranked.sort(key=lambda item: item.score, reverse=True)
        fallback_matches = ranked[: payload.limit]
        llm_limit = min(max(payload.limit, 3), 12)

        trace = DebugTrace(model_provider=route.provider_code, model_profile=route.profile_code or "")
        match_context = measure_step(
            trace,
            "candidate_recall",
            lambda: self.intelligence.build_match_context(payload),
        )
        trace.retrieval_hits = match_context.memory_hits[:4]
        trace.graph_facts = match_context.graph_facts[:4]

        llm_candidates = []
        shortlist = self._build_candidate_shortlist(payload, ranked, match_context.candidates)
        for candidate, item, retrieval_score, retrieval_evidence in shortlist:
            graph_facts = self.intelligence.graph_store.describe_relationship(payload.owner.user_id, candidate.user_id)
            if graph_facts and len(trace.graph_facts) < 8:
                trace.graph_facts.extend(graph_facts[:2])
            llm_candidates.append(
                {
                    "user_id": candidate.user_id,
                    "nickname": candidate.nickname,
                    "city": candidate.city,
                    "age": candidate.age,
                    "height": candidate.height,
                    "education": candidate.education,
                    "job": candidate.job,
                    "summary": candidate.summary,
                    "tags": candidate.tags,
                    "interests": candidate.interests,
                    "verified": candidate.verified,
                    "likes_owner": candidate.likes_owner,
                    "liked_by_owner": candidate.liked_by_owner,
                    "heuristic_score": item.score,
                    "heuristic_summary": item.summary,
                    "retrieval_score": round(retrieval_score, 4),
                    "retrieval_evidence": retrieval_evidence,
                    "fit_tags": item.fit_tags,
                    "graph_facts": graph_facts,
                }
            )

        llm_result = measure_step(
            trace,
            "llm_rank",
            lambda: self.llm.complete_json(
                route=route,
                system_prompt=(
                    "你是丘偶的智能红娘 Agent。"
                    "你必须只输出 JSON，不要 markdown，不要解释。"
                    "输出格式必须是 {insight, matches:[{user_id, score, summary, fit_tags, reasons, icebreak_openers, recommended_action}], reasoning_summary}。"
                    "只能从给定 candidate user_id 里选人。"
                    "每个 matches 项 reasons 至少 2 条，icebreak_openers 至少 1 条。"
                    "推荐文案要符合中文婚恋社交产品语境，突出为什么适合破冰。"
                ),
                user_payload={
                    "owner": payload.owner.model_dump(mode="json"),
                    "preference": payload.preference.model_dump(mode="json"),
                    "limit": llm_limit,
                    "retrieval_hits": trace.retrieval_hits,
                    "retrieval_status": "no_result" if not trace.retrieval_hits else "hit",
                    "candidates": llm_candidates,
                },
                fallback=self._fallback_payload(fallback_matches, payload, llm_limit),
            ),
        )

        matches = self._merge_llm_matches(llm_result, fallback_matches, payload.limit, trace.trace_id)
        insight = ""
        reasoning_summary = ""
        if isinstance(llm_result, dict):
            insight = str(llm_result.get("insight") or "").strip()
            reasoning_summary = str(llm_result.get("reasoning_summary") or "").strip()
        if not insight:
            insight = self._build_insight(payload, matches)
        trace.reasoning_summary = reasoning_summary or "推荐结果基于偏好、记忆召回、关系事实与候选画像重排生成。"

        response = SmartMatchResponse(
            provider=route.provider_code,
            route_profile=route.profile_code,
            insight=insight,
            matches=matches,
            debug_trace_id=trace.trace_id,
            retrieval_hits=trace.retrieval_hits,
            graph_facts=trace.graph_facts,
            reasoning_summary=trace.reasoning_summary,
        )
        self.intelligence.record_match_recommendation(payload, matches)
        self.trace_store.record(
            agent_type="matchmaker",
            scene_code=scene_code,
            function_type=function_type,
            trace=trace,
            owner_user_id=payload.owner.user_id if payload.owner else None,
            target_user_id=matches[0].user_id if matches else None,
            request_json={
                "owner": payload.owner.model_dump(mode="json"),
                "preference": payload.preference.model_dump(mode="json"),
                "limit": payload.limit,
                "candidate_count": len(payload.candidates),
                "llm_candidates": llm_candidates,
            },
            response_json=response.model_dump(mode="json"),
            request_summary=f"match owner={payload.owner.user_id} candidates={len(payload.candidates)} limit={payload.limit}",
            response_summary=insight or (matches[0].summary if matches else ""),
        )
        return apply_debug_visibility(response, self.intelligence.settings.llm_enable_debug_trace)

    def _build_candidate_shortlist(
        self,
        payload: SmartMatchRequest,
        ranked: list[SmartMatchItem],
        retrieved_candidates,
    ) -> list[tuple[MatchCandidate, SmartMatchItem, float, list[str]]]:
        ranked_map = {item.user_id: item for item in ranked}
        candidate_map = {candidate.user_id: candidate for candidate in payload.candidates}
        shortlist: list[tuple[MatchCandidate, SmartMatchItem, float, list[str]]] = []
        seen: set[int] = set()

        for retrieved in retrieved_candidates:
            candidate = candidate_map.get(retrieved.candidate.user_id)
            heuristic = ranked_map.get(retrieved.candidate.user_id)
            if candidate is None or heuristic is None or candidate.user_id in seen:
                continue
            shortlist.append((candidate, heuristic, retrieved.score, retrieved.evidence[:3]))
            seen.add(candidate.user_id)
            if len(shortlist) >= min(max(payload.limit * 3, 6), 12):
                return shortlist

        for heuristic in ranked:
            candidate = candidate_map.get(heuristic.user_id)
            if candidate is None or candidate.user_id in seen:
                continue
            shortlist.append((candidate, heuristic, 0.0, []))
            seen.add(candidate.user_id)
            if len(shortlist) >= min(max(payload.limit * 3, 6), 12):
                break
        return shortlist

    def _merge_llm_matches(
        self,
        llm_result: dict | None,
        fallback_matches: list[SmartMatchItem],
        limit: int,
        trace_id: str,
    ) -> list[SmartMatchItem]:
        fallback_map = {item.user_id: item for item in fallback_matches}
        merged: list[SmartMatchItem] = []
        if isinstance(llm_result, dict) and isinstance(llm_result.get("matches"), list):
            for item in llm_result["matches"]:
                if not isinstance(item, dict):
                    continue
                user_id = int(item.get("user_id") or 0)
                if user_id <= 0 or user_id not in fallback_map:
                    continue
                fallback = fallback_map[user_id]
                merged.append(
                    SmartMatchItem(
                        user_id=user_id,
                        score=max(36, min(99, int(item.get("score") or fallback.score))),
                        summary=self._pick_text(item.get("summary"), fallback.summary),
                        fit_tags=self._pick_list(item.get("fit_tags"), fallback.fit_tags, 4),
                        reasons=self._ensure_reasons(item.get("reasons"), fallback.reasons),
                        icebreak_openers=self._ensure_openers(item.get("icebreak_openers"), fallback),
                        recommended_action=self._pick_text(item.get("recommended_action"), fallback.recommended_action),
                        debug_trace_id=trace_id,
                    )
                )
                if len(merged) >= limit:
                    break
        if len(merged) < limit:
            existing = {item.user_id for item in merged}
            for fallback in fallback_matches:
                if fallback.user_id in existing:
                    continue
                merged.append(
                    SmartMatchItem(
                        user_id=fallback.user_id,
                        score=fallback.score,
                        summary=fallback.summary,
                        fit_tags=fallback.fit_tags,
                        reasons=self._ensure_reasons(fallback.reasons, fallback.reasons),
                        icebreak_openers=self._ensure_openers([], fallback),
                        recommended_action=fallback.recommended_action,
                        debug_trace_id=trace_id,
                    )
                )
                if len(merged) >= limit:
                    break
        return merged

    def _fallback_payload(self, matches: list[SmartMatchItem], payload: SmartMatchRequest, llm_limit: int) -> dict:
        return {
            "insight": self._build_insight(payload, matches),
            "reasoning_summary": "推荐结果基于偏好条件、资料完整度、活跃度与互动信号综合排序。",
            "matches": [item.model_dump(mode="json") for item in matches[:llm_limit]],
        }

    def _rank_candidate(self, payload: SmartMatchRequest, candidate: MatchCandidate) -> SmartMatchItem:
        preference = payload.preference
        owner_tags = self._normalize_tokens(payload.owner.tags)
        owner_tags.update(self._tokenize(payload.owner.summary))
        candidate_tags = self._normalize_tokens(candidate.tags)
        candidate_tags.update(self._normalize_tokens(candidate.interests))
        candidate_tags.update(self._tokenize(candidate.summary))

        score = 32.0
        reasons: list[str] = []
        fit_tags: list[str] = []

        common_tags = sorted(owner_tags & candidate_tags)
        if common_tags:
            score += min(18, len(common_tags) * 6)
            top_tags = common_tags[:2]
            fit_tags.extend(top_tags)
            reasons.append(f"你们都在意{'、'.join(top_tags)}这类话题，开场不容易冷。")

        city_match = self._normalize_text(candidate.city) in {
            self._normalize_text(city) for city in preference.preferred_cities if city
        }
        owner_city_match = self._normalize_text(candidate.city) == self._normalize_text(payload.owner.city)
        if city_match or owner_city_match:
            score += 12
            city_name = candidate.city or payload.owner.city or "同城"
            reasons.append(f"{city_name}优先，更容易从线上聊到线下见面。")
            if city_name:
                fit_tags.append(city_name)

        age_score, age_reason = self._score_range(candidate.age, preference.age_min, preference.age_max, "年龄")
        score += age_score
        if age_reason:
            reasons.append(age_reason)

        height_score, height_reason = self._score_range(candidate.height, preference.height_min, preference.height_max, "身高")
        score += height_score
        if height_reason:
            reasons.append(height_reason)

        if preference.education_levels:
            if candidate.education in preference.education_levels:
                score += 10
                reasons.append("学历偏好匹配，长期沟通成本相对更低。")
            elif candidate.education is not None:
                score += 2
        elif candidate.education is not None:
            score += 4

        if candidate.verified:
            score += 6
            reasons.append("资料可信度更高，主理人更适合优先推荐。")
        elif candidate.identity_verified or candidate.education_verified:
            score += 4

        if candidate.likes_owner:
            score += 9
            fit_tags.append("对你有关注")
            reasons.append("对方已经对你有过关注信号，推进成功率更高。")
        if candidate.liked_by_owner:
            score += 6
            fit_tags.append("你已心动")
            reasons.append("你之前已经表达过好感，继续推进会更自然。")
        if preference.only_liked and candidate.liked_by_owner:
            score += 3

        if candidate.completeness > 0:
            score += min(8, candidate.completeness / 12)
        if candidate.last_active_hours is not None:
            if candidate.last_active_hours <= 6:
                score += 8
                fit_tags.append("最近活跃")
            elif candidate.last_active_hours <= 24:
                score += 6
            elif candidate.last_active_hours <= 72:
                score += 3

        if candidate.vip:
            score += 2

        reasons = reasons[:3] or ["资料完整、关系推进风险较低，适合优先看看。"]
        fit_tags = fit_tags[:4]
        final_score = max(36, min(98, int(round(score))))
        summary = self._build_summary(candidate, reasons, final_score)
        return SmartMatchItem(
            user_id=candidate.user_id,
            score=final_score,
            summary=summary,
            fit_tags=fit_tags,
            reasons=reasons,
            icebreak_openers=self._fallback_openers(candidate, reasons),
            recommended_action="start_chat" if final_score >= 72 else "open_profile",
        )

    def _build_summary(self, candidate: MatchCandidate, reasons: list[str], score: int) -> str:
        headline = candidate.nickname or "这位用户"
        if score >= 85:
            return f"{headline}是当前这批里更值得优先推进的一位，适合先认真打开话题。"
        if score >= 72:
            return f"{headline}和你的基础契合度不错，比较适合先聊再看节奏。"
        return f"{headline}可以先看看主页和动态，再决定要不要进一步认识。"

    def _build_insight(self, payload: SmartMatchRequest, matches: list[SmartMatchItem]) -> str:
        if not matches:
            return "这次没有筛到特别稳妥的人选，建议先放宽城市或学历条件再试。"
        cities = [city for city in payload.preference.preferred_cities if city]
        city_part = f"优先看{cities[0]}" if cities else "先按你当前资料和活跃度筛"
        return f"{city_part}、年龄学历更贴近的对象，并把更容易开口的人排在前面。"

    def _build_owner_query(self, payload: SmartMatchRequest) -> str:
        parts = [
            payload.owner.nickname or "",
            payload.owner.city or "",
            payload.owner.summary or "",
            " ".join(payload.owner.tags),
            " ".join(payload.preference.preferred_cities),
        ]
        return " ".join(item for item in parts if item).strip()

    def _fallback_openers(self, candidate: MatchCandidate, reasons: list[str]) -> list[str]:
        topic = (candidate.interests or candidate.tags or ["最近的生活"])[0]
        nickname = candidate.nickname or "你"
        opening = f"看到你资料里提到{topic}，这个点一下让我记住你了，最近你还会花时间在这件事上吗？"
        follow_up = f"刚看到你，感觉你是那种适合慢慢聊开的人，所以想认真和{nickname}打个招呼。"
        if reasons:
            follow_up = f"看到你资料挺完整的，而且{reasons[0].replace('。', '')}，就想来认识一下。"
        return [opening, follow_up]

    def _ensure_reasons(self, value, fallback: list[str]) -> list[str]:
        if isinstance(value, list):
            reasons = [str(item).strip() for item in value if str(item).strip()]
            if len(reasons) >= 2:
                return reasons[:3]
            if reasons:
                return (reasons + fallback)[:3]
        return (fallback or ["资料完整、关系推进风险较低，适合优先看看。"])[:3]

    def _ensure_openers(self, value, fallback: SmartMatchItem) -> list[str]:
        if isinstance(value, list):
            openers = [str(item).strip() for item in value if str(item).strip()]
            if openers:
                return openers[:3]
        return fallback.icebreak_openers[:2] or ["你好呀，我觉得我们可以先从最近的生活聊起。"]

    def _score_range(self, value: int | None, lower: int | None, upper: int | None, label: str) -> tuple[float, str]:
        if value is None or lower is None or upper is None:
            return 0.0, ""
        if lower > upper:
            lower, upper = upper, lower
        if lower <= value <= upper:
            midpoint = (lower + upper) / 2
            distance = abs(value - midpoint)
            span = max(1.0, (upper - lower) / 2)
            score = 4 + max(0.0, 8 * (1 - distance / span))
            return score, f"{label}落在你的偏好区间里，基础接受度更高。"
        distance = min(abs(value - lower), abs(value - upper))
        if distance <= 2:
            return 2.0, f"{label}虽然略有偏差，但还在可接受边缘。"
        return -4.0, ""

    def _tokenize(self, value: str | None) -> set[str]:
        text = self._normalize_text(value)
        if not text:
            return set()
        chunks = [item for item in re.split(r"[，,。；;、/\s]+", text) if len(item) >= 2]
        return {item[:8] for item in chunks[:8]}

    def _normalize_tokens(self, values: list[str]) -> set[str]:
        return {self._normalize_text(value) for value in values if self._normalize_text(value)}

    def _normalize_text(self, value: str | None) -> str:
        return re.sub(r"\s+", "", str(value or "").strip().lower())

    def _pick_text(self, value, fallback: str) -> str:
        text = str(value or "").strip()
        return text or fallback

    def _pick_list(self, value, fallback: list[str], limit: int) -> list[str]:
        if isinstance(value, list):
            normalized = [str(item).strip() for item in value if str(item).strip()]
            if normalized:
                return normalized[:limit]
        return fallback[:limit]
