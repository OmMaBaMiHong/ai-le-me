from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


class RuntimeTraceListItem(BaseModel):
    trace_id: str
    agent_type: str
    scene_code: str
    function_type: str
    owner_user_id: int | None = None
    target_user_id: int | None = None
    model_provider: str = ""
    model_profile: str = ""
    status: str = "success"
    request_summary: str = ""
    response_summary: str = ""
    reasoning_summary: str = ""
    error_message: str = ""
    created_at: datetime


class RuntimeTracePageResponse(BaseModel):
    total: int = 0
    page: int = 1
    page_size: int = 20
    items: list[RuntimeTraceListItem] = Field(default_factory=list)


class RuntimeTraceDetail(RuntimeTraceListItem):
    retrieval_hits: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    timing_breakdown: dict[str, float] = Field(default_factory=dict)
    request_json: dict[str, object] = Field(default_factory=dict)
    response_json: dict[str, object] = Field(default_factory=dict)
