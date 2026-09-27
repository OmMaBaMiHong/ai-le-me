from __future__ import annotations

from agent_runtime.schemas.autonomy import CoachStyleSelectionRequest, CoachStyleSelectionResponse


class CoachStyleService:
    STYLE_META = {
        "warm_guardian": {
            "style_name": "温柔安全感型",
            "tone_hint": "低压、稳定、给安全感",
            "opening_hint": "先承接情绪与日常，再给轻问句",
        },
        "steady_partner": {
            "style_name": "成熟稳重型",
            "tone_hint": "真诚、克制、可靠",
            "opening_hint": "用生活感和兑现感建立信任",
        },
        "life_companion": {
            "style_name": "生活感陪伴型",
            "tone_hint": "自然、松弛、像真实生活里的人",
            "opening_hint": "围绕同城、日常、共同兴趣推进",
        },
        "playful_tease": {
            "style_name": "轻松幽默型",
            "tone_hint": "轻松、带一点俏皮、不油腻",
            "opening_hint": "用轻逗趣打破拘谨，但不过度调情",
        },
        "invitation_driver": {
            "style_name": "低压邀约型",
            "tone_hint": "自然推进、不强迫、带可选项",
            "opening_hint": "在 readiness 足够时给轻邀约或未来计划",
        },
    }

    def select(self, request: CoachStyleSelectionRequest) -> CoachStyleSelectionResponse:
        kernel = request.persona_kernel
        kernel_text = self._kernel_text(request)
        summary = f"{request.persona_summary} {kernel_text} {' '.join(request.relationship_tags)} {' '.join(request.graph_facts)} {' '.join(request.target.tags)}".lower()
        blocked: list[str] = []
        preferred_style = kernel.preferred_master_style_code if kernel else ""

        if preferred_style in {"warm_guardian", "steady_partner", "life_companion", "invitation_driver", "playful_tease"}:
            style_code = preferred_style
        elif any(token in summary for token in ("慢热", "安全感", "steady", "safe")):
            style_code = "warm_guardian"
            blocked.extend(["playful_tease"])
        elif request.date_ready_score >= 0.75:
            style_code = "invitation_driver"
            blocked.extend(["warm_guardian"])
        elif any(token in summary for token in ("同城", "生活", "日常", "city")):
            style_code = "life_companion"
            blocked.extend(["romantic_signal"])
        elif request.wechat_ready_score >= 0.55:
            style_code = "steady_partner"
            blocked.extend(["playful_tease"])
        else:
            style_code = "steady_partner"
            blocked.extend(["playful_tease"])

        if request.stage_code in {"date_ready", "date_proposed"} and style_code not in {"invitation_driver", "life_companion"}:
            style_code = "life_companion" if request.date_ready_score < 0.78 else "invitation_driver"

        meta = self.STYLE_META[style_code]
        reasoning = (
            f"stage={request.stage_code}; date_ready={request.date_ready_score:.2f}; "
            f"wechat_ready={request.wechat_ready_score:.2f}; selected={style_code}"
        )
        if preferred_style:
            reasoning += f"; persona_kernel={preferred_style}"
        if any(token in summary for token in ("邀约", "date_ready")):
            reasoning += "；当前已接近邀约窗口，避免过度试探。"
        elif "慢热" in summary or "安全感" in summary:
            reasoning += "；目标对象偏慢热或需要安全感，优先稳定推进。"

        return CoachStyleSelectionResponse(
            style_code=style_code,
            style_name=meta["style_name"],
            tone_hint=meta["tone_hint"],
            opening_hint=meta["opening_hint"],
            do_not_use_styles=blocked,
            reasoning_summary=reasoning,
        )

    def _kernel_text(self, request: CoachStyleSelectionRequest) -> str:
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
