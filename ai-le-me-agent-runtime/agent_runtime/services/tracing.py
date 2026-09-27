from __future__ import annotations

from dataclasses import dataclass, field
from time import perf_counter
from typing import Callable
from uuid import uuid4


@dataclass
class DebugTrace:
    trace_id: str = field(default_factory=lambda: uuid4().hex)
    model_provider: str = ""
    model_profile: str = ""
    retrieval_hits: list[str] = field(default_factory=list)
    graph_facts: list[str] = field(default_factory=list)
    reasoning_summary: str = ""
    timing_breakdown: dict[str, float] = field(default_factory=dict)

    def as_dict(self) -> dict[str, object]:
        return {
            "trace_id": self.trace_id,
            "model_provider": self.model_provider,
            "model_profile": self.model_profile,
            "retrieval_hits": self.retrieval_hits,
            "graph_facts": self.graph_facts,
            "reasoning_summary": self.reasoning_summary,
            "timing_breakdown": self.timing_breakdown,
        }


def measure_step(trace: DebugTrace, key: str, fn: Callable[[], object]):
    start = perf_counter()
    result = fn()
    trace.timing_breakdown[key] = round((perf_counter() - start) * 1000, 2)
    return result


def apply_debug_visibility(response, enabled: bool):
    if enabled:
        return response
    for field_name in ("retrieval_hits", "graph_facts"):
        if hasattr(response, field_name):
            setattr(response, field_name, [])
    return response
