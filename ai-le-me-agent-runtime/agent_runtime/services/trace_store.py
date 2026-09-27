from __future__ import annotations

from agent_runtime.repositories.trace import RuntimeTraceRecord, RuntimeTraceRepository
from agent_runtime.schemas.trace import RuntimeTraceDetail, RuntimeTracePageResponse
from agent_runtime.services.tracing import DebugTrace


class RuntimeTraceService:
    def __init__(self, repository: RuntimeTraceRepository) -> None:
        self.repository = repository

    def record(
        self,
        *,
        agent_type: str,
        scene_code: str,
        function_type: str,
        trace: DebugTrace,
        owner_user_id: int | None,
        target_user_id: int | None,
        request_json: dict[str, object],
        response_json: dict[str, object],
        request_summary: str,
        response_summary: str,
        status: str = "success",
        error_message: str = "",
    ) -> None:
        self.repository.save(
            RuntimeTraceRecord(
                trace_id=trace.trace_id,
                agent_type=agent_type,
                scene_code=scene_code,
                function_type=function_type,
                owner_user_id=owner_user_id,
                target_user_id=target_user_id,
                provider_code=trace.model_provider,
                profile_code=trace.model_profile,
                status=status,
                request_summary=request_summary,
                response_summary=response_summary,
                retrieval_hits=trace.retrieval_hits,
                graph_facts=trace.graph_facts,
                reasoning_summary=trace.reasoning_summary,
                timing_breakdown=trace.timing_breakdown,
                request_json=request_json,
                response_json=response_json,
                error_message=error_message,
            )
        )

    def list_traces(
        self,
        *,
        agent_type: str | None = None,
        owner_user_id: int | None = None,
        target_user_id: int | None = None,
        provider_code: str | None = None,
        trace_id: str | None = None,
        keyword: str | None = None,
        page: int = 1,
        page_size: int = 20,
    ) -> RuntimeTracePageResponse:
        total, rows = self.repository.list_traces(
            agent_type=agent_type,
            owner_user_id=owner_user_id,
            target_user_id=target_user_id,
            provider_code=provider_code,
            trace_id=trace_id,
            keyword=keyword,
            page=page,
            page_size=page_size,
        )
        return RuntimeTracePageResponse(total=total, page=page, page_size=page_size, items=rows)

    def get_trace(self, trace_id: str) -> RuntimeTraceDetail | None:
        row = self.repository.get_trace(trace_id)
        if row is None:
            return None
        return RuntimeTraceDetail(**row)
