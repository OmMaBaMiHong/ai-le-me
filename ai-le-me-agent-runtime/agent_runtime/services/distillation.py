from __future__ import annotations

from agent_runtime.schemas.distillation import (
    DistillationEvidenceCard,
    DistillationGenerateRequest,
    DistillationGenerateResponse,
    DistillationServiceHooks,
)


class DistillationService:
    def generate(self, payload: DistillationGenerateRequest) -> DistillationGenerateResponse:
        if payload.scene_type == "self_bootstrap":
            return self._build_self_bootstrap(payload)
        return self._build_private_person(payload)

    def _build_self_bootstrap(self, payload: DistillationGenerateRequest) -> DistillationGenerateResponse:
        subject_name = payload.subject.nickname or (payload.owner.nickname if payload.owner else "我自己")
        material_count = len(payload.materials)
        answer_count = len(payload.answers)
        keywords = self._collect_keywords(payload)
        focus = keywords[0] if keywords else "稳定回应"
        evidence_cards = self._build_evidence_cards(payload, scene_label="自我材料")
        return DistillationGenerateResponse(
            scene_type=payload.scene_type,
            subject_name=subject_name,
            relation_label=payload.relation_label,
            summary=f"{subject_name}当前更适合走真诚、低压力、能给安全感的关系节奏，先把“{focus}”稳定下来会更容易建立高质量连接。",
            core_insights=[
                f"已接入 {material_count} 份补充材料，画像不再只依赖问答。",
                "你对关系中的稳定反馈比较敏感，容易被持续回应而不是瞬时热情打动。",
                "更适合生活化、可兑现的表达方式，不适合高浓度模板话术。",
            ],
            interaction_guidance=[
                "资料文案优先写清楚你在关系里的节奏和边界，而不是堆砌标签。",
                "破冰建议更适合从真实生活感和轻问题切入，让对方更容易接话。",
                "后续如果再补聊天记录或截图，优先补能体现情绪反应和边界表达的内容。",
            ],
            risk_flags=[
                "当前截图类材料尚未做 OCR 深解析，部分结论仍依赖你提供的描述。",
                "如果样本主要来自单一时段，画像可能放大那一阶段的状态。",
            ],
            evidence_cards=evidence_cards,
            confidence_notes=[
                f"当前综合了 {answer_count} 条问答和 {material_count} 份素材信号。",
                "涉及文字直述的结论置信度更高，仅基于图片存在的推断置信度较低。",
            ],
            persona_kernel={
                "distillation_mode": "self_bootstrap",
                "focus_traits": keywords[:4] or ["稳定回应", "真诚表达"],
                "material_count": material_count,
                "answer_count": answer_count,
            },
            service_hooks=DistillationServiceHooks(
                recommended_opening_style="先以真实生活片段开场，再接一个容易回应的小问题。",
                matchmaker_style_hint="适合稳一点、少压迫感、强调真诚兑现的红娘风格。",
                assistant_guardrails=[
                    "不要过度热情轰炸式输出",
                    "先确认对方回应频率，再决定是否推进",
                ],
                profile_copy_hint="把“我需要稳定回应、关系节奏偏自然”写进资料，会比泛泛而谈更有效。",
            ),
        )

    def _build_private_person(self, payload: DistillationGenerateRequest) -> DistillationGenerateResponse:
        subject_name = payload.subject.nickname or "对方"
        keywords = self._collect_keywords(payload)
        conflict_signal = "断联回避"
        if any(word in keywords for word in ["消失", "断联", "回避", "转移话题"]):
            conflict_signal = "冲突后撤退"
        evidence_cards = self._build_evidence_cards(payload, scene_label="关系材料")
        return DistillationGenerateResponse(
            scene_type=payload.scene_type,
            subject_name=subject_name,
            relation_label=payload.relation_label,
            summary=f"{subject_name}在关系压力上来时更像是“{conflict_signal}”型，核心不是不在意，而是更倾向先抽离，再回到熟悉的相处表面。",
            core_insights=[
                "材料显示对方在涉及承诺、冲突或高压情绪时，倾向先撤退而不是正面处理。",
                "你们的问题更像节奏和处理冲突方式错位，不只是单次沟通说错话。",
                "如果继续互动，核心不是多解释，而是先降低压迫感并明确边界。",
            ],
            interaction_guidance=[
                "少做连续追问，先用单点、低压、可回应的问题测试对方是否愿意恢复连接。",
                "如果要复盘关系，优先描述自己的感受和边界，不要代替对方下结论。",
                "把是否愿意稳定回应作为关键观察指标，而不是只看短期热度回升。",
            ],
            risk_flags=[
                "私域人物蒸馏只适用于关系理解，不应用于操控、冒充或追踪。",
                "截图材料当前未做 OCR 深解析，涉及图片细节的判断仅作辅助信号。",
            ],
            evidence_cards=evidence_cards,
            confidence_notes=[
                f"本次基于 {len(payload.answers)} 条问答和 {len(payload.materials)} 份素材做综合判断。",
                "跨文字描述和素材标签重复出现的模式，置信度高于单一来源结论。",
            ],
            persona_kernel={
                "distillation_mode": "private_person_analysis",
                "relation_label": payload.relation_label or "private_person",
                "pattern_tags": keywords[:5] or ["慢热", "回避冲突"],
                "analysis_goal": payload.analysis_goal or "",
            },
            service_hooks=DistillationServiceHooks(
                recommended_opening_style="如果还要沟通，先用低压、非控诉式的生活化开场。",
                matchmaker_style_hint="这类对象不适合强推式撮合，更适合慢热观察和边界提醒。",
                assistant_guardrails=[
                    "不要给出操控或逼迫对方回头的建议",
                    "优先保护用户自己的情绪边界",
                ],
                profile_copy_hint="这份识人结果更适合做复盘和沟通参考，不直接写入公开资料。",
            ),
        )

    def _build_evidence_cards(
        self,
        payload: DistillationGenerateRequest,
        scene_label: str,
    ) -> list[DistillationEvidenceCard]:
        cards: list[DistillationEvidenceCard] = []
        if payload.answers:
            first_answer = payload.answers[0]
            cards.append(
                DistillationEvidenceCard(
                    kind="answer_signal",
                    title=first_answer.question_label or first_answer.question_code,
                    detail=first_answer.answer_text,
                    source_types=["answers"],
                )
            )
        material_types = sorted({item.material_type for item in payload.materials})
        if payload.materials:
            cards.append(
                DistillationEvidenceCard(
                    kind="material_signal",
                    title=f"{scene_label}已接入",
                    detail=f"已接入 {len(payload.materials)} 份素材，类型包括 {', '.join(material_types)}。",
                    source_types=material_types,
                )
            )
            text_material = next((item for item in payload.materials if item.content), None)
            if text_material is not None:
                cards.append(
                    DistillationEvidenceCard(
                        kind="material_excerpt",
                        title=text_material.label or "素材摘要",
                        detail=text_material.content[:120],
                        source_types=[text_material.material_type],
                    )
                )
        return cards[:4]

    def _collect_keywords(self, payload: DistillationGenerateRequest) -> list[str]:
        raw_texts: list[str] = []
        if payload.analysis_goal:
            raw_texts.append(payload.analysis_goal)
        raw_texts.extend(item.answer_text for item in payload.answers if item.answer_text)
        raw_texts.extend(item.content for item in payload.materials if item.content)
        raw_texts.extend(payload.owner.tags if payload.owner else [])
        raw_texts.extend(payload.subject.tags)

        keywords: list[str] = []
        for phrase in [
            "稳定回应",
            "真诚表达",
            "慢热",
            "安全感",
            "消失",
            "断联",
            "回避",
            "转移话题",
            "边界",
            "承诺",
        ]:
            if any(phrase in text for text in raw_texts) and phrase not in keywords:
                keywords.append(phrase)
        return keywords
