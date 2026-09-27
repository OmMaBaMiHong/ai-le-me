from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class EvalMetric:
    code: str
    name: str
    objective: str
    data_sources: tuple[str, ...] = field(default_factory=tuple)


class AgentEvalCatalog:
    def list_metrics(self) -> list[EvalMetric]:
        return [
            EvalMetric(
                code="reply_acceptance_rate",
                name="Reply Acceptance Rate",
                objective="Measure whether suggested or automated replies sustain healthy conversations.",
                data_sources=("chat_turns", "assistant_actions", "followup_engagement"),
            ),
            EvalMetric(
                code="wechat_exchange_rate",
                name="WeChat Exchange Rate",
                objective="Measure whether relationship progression reaches contact exchange at acceptable risk.",
                data_sources=("relationship_stage", "assistant_actions", "conversion_events"),
            ),
            EvalMetric(
                code="date_conversion_rate",
                name="Date Conversion Rate",
                objective="Measure whether multi-step progression leads to successful date scheduling.",
                data_sources=("assistant_plans", "date_events", "chat_turns"),
            ),
            EvalMetric(
                code="policy_intercept_rate",
                name="Policy Intercept Rate",
                objective="Measure how often the policy layer blocks risky or disallowed actions.",
                data_sources=("policy_decisions", "action_attempts"),
            ),
            EvalMetric(
                code="cost_per_success",
                name="Cost Per Successful Progression",
                objective="Measure external model and execution cost against meaningful relationship outcomes.",
                data_sources=("token_usage", "billing_records", "conversion_events"),
            ),
            EvalMetric(
                code="retrieval_usefulness",
                name="Retrieval Usefulness",
                objective="Measure whether recalled evidence improves quality, safety, and progression success.",
                data_sources=("retrieval_hits", "trace_feedback", "conversion_events"),
            ),
        ]
