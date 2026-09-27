from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime

from sqlalchemy import text

from agent_runtime.core.config import get_settings
from agent_runtime.core.db import get_engine


TRACE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS `agent_runtime_trace` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` varchar(64) NOT NULL COMMENT '调试trace_id',
  `agent_type` varchar(32) NOT NULL COMMENT 'persona/matchmaker/companion',
  `scene_code` varchar(64) NOT NULL COMMENT 'scene code',
  `function_type` varchar(64) NOT NULL COMMENT 'function type',
  `owner_user_id` bigint DEFAULT NULL COMMENT '主人用户ID',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID',
  `provider_code` varchar(64) DEFAULT NULL COMMENT '命中provider',
  `profile_code` varchar(64) DEFAULT NULL COMMENT '命中profile',
  `status` varchar(32) NOT NULL DEFAULT 'success' COMMENT '运行状态',
  `request_summary` varchar(1000) DEFAULT NULL COMMENT '请求摘要',
  `response_summary` varchar(1000) DEFAULT NULL COMMENT '响应摘要',
  `retrieval_hits_json` longtext COMMENT '召回命中',
  `graph_facts_json` longtext COMMENT '图谱事实',
  `reasoning_summary` varchar(2000) DEFAULT NULL COMMENT '推理摘要',
  `timing_breakdown_json` longtext COMMENT '耗时拆解',
  `request_json` longtext COMMENT '请求快照',
  `response_json` longtext COMMENT '响应快照',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace_id` (`trace_id`),
  KEY `idx_agent_time` (`agent_type`, `created_at`),
  KEY `idx_owner_time` (`owner_user_id`, `created_at`),
  KEY `idx_target_time` (`target_user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一Agent调试Trace';
"""


@dataclass
class RuntimeTraceRecord:
    trace_id: str
    agent_type: str
    scene_code: str
    function_type: str
    owner_user_id: int | None = None
    target_user_id: int | None = None
    provider_code: str = ""
    profile_code: str = ""
    status: str = "success"
    request_summary: str = ""
    response_summary: str = ""
    retrieval_hits: list[str] | None = None
    graph_facts: list[str] | None = None
    reasoning_summary: str = ""
    timing_breakdown: dict[str, float] | None = None
    request_json: dict[str, object] | None = None
    response_json: dict[str, object] | None = None
    error_message: str = ""


class RuntimeTraceRepository:
    def __init__(self) -> None:
        self.settings = get_settings()
        self._table_ready = False

    def save(self, record: RuntimeTraceRecord) -> None:
        if not self.settings.mysql_enable_runtime_tables:
            return
        self._ensure_table()
        sql = text(
            """
            INSERT INTO agent_runtime_trace (
                trace_id, agent_type, scene_code, function_type,
                owner_user_id, target_user_id, provider_code, profile_code, status,
                request_summary, response_summary, retrieval_hits_json, graph_facts_json,
                reasoning_summary, timing_breakdown_json, request_json, response_json,
                error_message
            ) VALUES (
                :trace_id, :agent_type, :scene_code, :function_type,
                :owner_user_id, :target_user_id, :provider_code, :profile_code, :status,
                :request_summary, :response_summary, :retrieval_hits_json, :graph_facts_json,
                :reasoning_summary, :timing_breakdown_json, :request_json, :response_json,
                :error_message
            )
            ON DUPLICATE KEY UPDATE
                agent_type = VALUES(agent_type),
                scene_code = VALUES(scene_code),
                function_type = VALUES(function_type),
                owner_user_id = VALUES(owner_user_id),
                target_user_id = VALUES(target_user_id),
                provider_code = VALUES(provider_code),
                profile_code = VALUES(profile_code),
                status = VALUES(status),
                request_summary = VALUES(request_summary),
                response_summary = VALUES(response_summary),
                retrieval_hits_json = VALUES(retrieval_hits_json),
                graph_facts_json = VALUES(graph_facts_json),
                reasoning_summary = VALUES(reasoning_summary),
                timing_breakdown_json = VALUES(timing_breakdown_json),
                request_json = VALUES(request_json),
                response_json = VALUES(response_json),
                error_message = VALUES(error_message)
            """
        )
        payload = {
            "trace_id": record.trace_id,
            "agent_type": record.agent_type,
            "scene_code": record.scene_code,
            "function_type": record.function_type,
            "owner_user_id": record.owner_user_id,
            "target_user_id": record.target_user_id,
            "provider_code": record.provider_code,
            "profile_code": record.profile_code,
            "status": record.status,
            "request_summary": self._shorten(record.request_summary, 1000),
            "response_summary": self._shorten(record.response_summary, 1000),
            "retrieval_hits_json": self._to_json(record.retrieval_hits or []),
            "graph_facts_json": self._to_json(record.graph_facts or []),
            "reasoning_summary": self._shorten(record.reasoning_summary, 2000),
            "timing_breakdown_json": self._to_json(record.timing_breakdown or {}),
            "request_json": self._to_json(record.request_json or {}),
            "response_json": self._to_json(record.response_json or {}),
            "error_message": self._shorten(record.error_message, 1000),
        }
        try:
            with get_engine().begin() as conn:
                conn.execute(sql, payload)
        except Exception:
            return

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
    ) -> tuple[int, list[dict[str, object]]]:
        if not self.settings.mysql_enable_runtime_tables:
            return 0, []
        self._ensure_table()
        where_sql, params = self._build_filters(
            agent_type=agent_type,
            owner_user_id=owner_user_id,
            target_user_id=target_user_id,
            provider_code=provider_code,
            trace_id=trace_id,
            keyword=keyword,
        )
        count_sql = text(f"SELECT COUNT(1) AS total FROM agent_runtime_trace {where_sql}")
        list_sql = text(
            f"""
            SELECT trace_id, agent_type, scene_code, function_type, owner_user_id, target_user_id,
                   provider_code, profile_code, status, request_summary, response_summary,
                   reasoning_summary, error_message, created_at
            FROM agent_runtime_trace
            {where_sql}
            ORDER BY created_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """
        )
        params["limit"] = max(1, min(page_size, 100))
        params["offset"] = max(page - 1, 0) * params["limit"]
        try:
            with get_engine().connect() as conn:
                total = int((conn.execute(count_sql, params).mappings().first() or {}).get("total") or 0)
                rows = conn.execute(list_sql, params).mappings().all()
        except Exception:
            return 0, []
        return total, [self._normalize_row(dict(row)) for row in rows]

    def get_trace(self, trace_id: str) -> dict[str, object] | None:
        if not self.settings.mysql_enable_runtime_tables or not trace_id:
            return None
        self._ensure_table()
        sql = text(
            """
            SELECT trace_id, agent_type, scene_code, function_type, owner_user_id, target_user_id,
                   provider_code, profile_code, status, request_summary, response_summary,
                   retrieval_hits_json, graph_facts_json, reasoning_summary, timing_breakdown_json,
                   request_json, response_json, error_message, created_at
            FROM agent_runtime_trace
            WHERE trace_id = :trace_id
            LIMIT 1
            """
        )
        try:
            with get_engine().connect() as conn:
                row = conn.execute(sql, {"trace_id": trace_id}).mappings().first()
        except Exception:
            return None
        if not row:
            return None
        return self._normalize_row(dict(row), with_payload=True)

    def _ensure_table(self) -> None:
        if self._table_ready:
            return
        try:
            with get_engine().begin() as conn:
                conn.execute(text(TRACE_TABLE_SQL))
            self._table_ready = True
        except Exception:
            return

    def _build_filters(
        self,
        *,
        agent_type: str | None,
        owner_user_id: int | None,
        target_user_id: int | None,
        provider_code: str | None,
        trace_id: str | None,
        keyword: str | None,
    ) -> tuple[str, dict[str, object]]:
        conditions: list[str] = ["1 = 1"]
        params: dict[str, object] = {}
        if agent_type:
            conditions.append("agent_type = :agent_type")
            params["agent_type"] = agent_type.strip().lower()
        if owner_user_id:
            conditions.append("owner_user_id = :owner_user_id")
            params["owner_user_id"] = owner_user_id
        if target_user_id:
            conditions.append("target_user_id = :target_user_id")
            params["target_user_id"] = target_user_id
        if provider_code:
            conditions.append("provider_code = :provider_code")
            params["provider_code"] = provider_code.strip().lower()
        if trace_id:
            conditions.append("trace_id = :trace_id")
            params["trace_id"] = trace_id.strip()
        if keyword:
            conditions.append(
                "(request_summary LIKE :keyword OR response_summary LIKE :keyword "
                "OR reasoning_summary LIKE :keyword OR error_message LIKE :keyword)"
            )
            params["keyword"] = f"%{keyword.strip()}%"
        return "WHERE " + " AND ".join(conditions), params

    def _normalize_row(self, row: dict[str, object], with_payload: bool = False) -> dict[str, object]:
        normalized = {
            "trace_id": str(row.get("trace_id") or ""),
            "agent_type": str(row.get("agent_type") or ""),
            "scene_code": str(row.get("scene_code") or ""),
            "function_type": str(row.get("function_type") or ""),
            "owner_user_id": self._to_int(row.get("owner_user_id")),
            "target_user_id": self._to_int(row.get("target_user_id")),
            "model_provider": str(row.get("provider_code") or ""),
            "model_profile": str(row.get("profile_code") or ""),
            "status": str(row.get("status") or "success"),
            "request_summary": str(row.get("request_summary") or ""),
            "response_summary": str(row.get("response_summary") or ""),
            "reasoning_summary": str(row.get("reasoning_summary") or ""),
            "error_message": str(row.get("error_message") or ""),
            "created_at": row.get("created_at") or datetime.utcnow(),
        }
        if not with_payload:
            return normalized
        normalized["retrieval_hits"] = self._from_json(row.get("retrieval_hits_json"), [])
        normalized["graph_facts"] = self._from_json(row.get("graph_facts_json"), [])
        normalized["timing_breakdown"] = self._from_json(row.get("timing_breakdown_json"), {})
        normalized["request_json"] = self._from_json(row.get("request_json"), {})
        normalized["response_json"] = self._from_json(row.get("response_json"), {})
        return normalized

    def _to_json(self, value: object) -> str:
        return json.dumps(value, ensure_ascii=False)

    def _from_json(self, value: object, fallback):
        if not value:
            return fallback
        try:
            return json.loads(str(value))
        except (TypeError, ValueError):
            return fallback

    def _to_int(self, value: object) -> int | None:
        try:
            return int(value) if value is not None else None
        except (TypeError, ValueError):
            return None

    def _shorten(self, text_value: str, limit: int) -> str:
        value = str(text_value or "").strip()
        if len(value) <= limit:
            return value
        return value[: max(limit - 3, 0)] + "..."
