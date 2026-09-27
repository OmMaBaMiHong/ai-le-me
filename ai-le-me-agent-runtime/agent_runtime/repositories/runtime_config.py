from __future__ import annotations

import json
from dataclasses import dataclass, field
from typing import Dict, Optional

from sqlalchemy import text

from agent_runtime.core.config import get_settings
from agent_runtime.core.db import get_engine


@dataclass
class ResolvedRoute:
    service_type: str
    provider_code: str
    profile_code: Optional[str] = None
    route_rule_id: Optional[int] = None
    configs: dict[str, str] = field(default_factory=dict)


class RuntimeConfigRepository:
    def __init__(self) -> None:
        self.settings = get_settings()

    def get_current_provider_code(self, service_type: str, fallback_code: str) -> str:
        if not self.settings.mysql_read_shared_config:
            return fallback_code
        sql = text(
            f"""
            SELECT provider_code
            FROM {self.settings.third_party_provider_table}
            WHERE service_type = :service_type AND is_current = 1 AND is_enabled = 1
            ORDER BY display_order ASC, provider_id ASC
            LIMIT 1
            """
        )
        try:
            with get_engine().connect() as conn:
                row = conn.execute(sql, {"service_type": service_type}).mappings().first()
        except Exception:
            return fallback_code
        if not row or not row.get("provider_code"):
            return fallback_code
        return self._normalize_code(str(row["provider_code"]))

    def get_provider_configs(
        self,
        service_type: str,
        provider_code: str,
        profile_code: Optional[str] = None,
    ) -> dict[str, str]:
        configs = self._load_profile_configs(service_type, provider_code, profile_code)
        if self._has_usable_llm_config(configs):
            return configs
        env_fallback = self._build_env_default_configs(service_type, provider_code, profile_code)
        return env_fallback or configs

    def resolve_route(
        self,
        service_type: str,
        route_context: Optional[Dict[str, str]],
        fallback_provider: str,
    ) -> ResolvedRoute:
        route_context = route_context or {}
        env_route = self._build_env_default_route(service_type)
        if env_route is not None and self.settings.llm_prefer_env_default:
            return env_route
        current_provider = self.get_current_provider_code(service_type, fallback_provider)
        rules = self._load_route_rules(service_type)
        for rule in rules:
            if not self._matches(rule, route_context):
                continue
            provider = self._normalize_code(rule.get("provider_code") or current_provider)
            profile = self._normalize_code(rule.get("profile_code"))
            configs = self.get_provider_configs(service_type, provider, profile)
            if not profile:
                profile = self._normalize_code(configs.get("profile_code"))
            resolved = ResolvedRoute(
                service_type=service_type,
                provider_code=provider,
                profile_code=profile or None,
                route_rule_id=rule.get("route_rule_id"),
                configs=configs,
            )
            if self._has_usable_llm_config(resolved.configs):
                return resolved
            if env_route is not None:
                return env_route
            return resolved
        configs = self.get_provider_configs(service_type, current_provider)
        profile = self._normalize_code(configs.get("profile_code"))
        resolved = ResolvedRoute(
            service_type=service_type,
            provider_code=current_provider,
            profile_code=profile or None,
            configs=configs,
        )
        if self._has_usable_llm_config(resolved.configs):
            return resolved
        return env_route or resolved

    def _build_env_default_route(self, service_type: str) -> ResolvedRoute | None:
        configs = self._build_env_default_configs(
            service_type,
            self.settings.llm_default_provider_code,
            self.settings.llm_default_profile_code,
        )
        if not configs:
            return None
        return ResolvedRoute(
            service_type=service_type,
            provider_code=self._normalize_code(self.settings.llm_default_provider_code),
            profile_code=self._normalize_code(self.settings.llm_default_profile_code) or None,
            configs=configs,
        )

    def _build_env_default_configs(
        self,
        service_type: str,
        provider_code: str,
        profile_code: Optional[str],
    ) -> dict[str, str]:
        if service_type != self.settings.default_ai_service_type:
            return {}
        api_key = str(self.settings.llm_default_api_key or "").strip()
        endpoint = str(self.settings.llm_default_endpoint or "").strip().rstrip("/")
        model = str(self.settings.llm_default_model or "").strip()
        if not api_key or not endpoint or not model:
            return {}
        return {
            "provider_code": self._normalize_code(provider_code),
            "profile_code": self._normalize_code(profile_code or self.settings.llm_default_profile_code),
            "endpoint": endpoint,
            "api_key": api_key,
            "model": model,
            "api_protocol": str(self.settings.llm_default_api_protocol or "responses").strip(),
            "reasoning_effort": str(self.settings.llm_default_reasoning_effort or "medium").strip(),
            "max_output_tokens": str(max(256, self.settings.llm_default_max_output_tokens)),
            "timeout": str(max(self.settings.llm_request_timeout, 45)),
        }

    def _has_usable_llm_config(self, configs: dict[str, str]) -> bool:
        endpoint = str(configs.get("endpoint") or "").strip()
        model = str(configs.get("model") or configs.get("model_name") or "").strip()
        api_key = str(configs.get("api_key") or "").strip()
        return bool(endpoint and model and api_key)

    def _load_profile_configs(
        self,
        service_type: str,
        provider_code: str,
        profile_code: Optional[str],
    ) -> dict[str, str]:
        if not self.settings.mysql_read_shared_config:
            return {}
        sql = text(
            f"""
            SELECT config_json
            FROM {self.settings.third_party_provider_table}
            WHERE service_type = :service_type AND provider_code = :provider_code AND is_enabled = 1
            LIMIT 1
            """
        )
        try:
            with get_engine().connect() as conn:
                row = conn.execute(
                    sql,
                    {"service_type": service_type, "provider_code": provider_code},
                ).mappings().first()
        except Exception:
            return {}
        raw = row.get("config_json") if row else None
        if not raw:
            return {}
        try:
            config_doc = json.loads(raw)
        except (TypeError, json.JSONDecodeError):
            return {}
        resolved: dict[str, str] = {}
        for key, value in config_doc.items():
            if key == "profiles":
                continue
            resolved[key] = self._stringify(value)
        profiles = config_doc.get("profiles") or []
        wanted = self._normalize_code(profile_code or config_doc.get("defaultProfile"))
        for profile in profiles:
            profile_name = self._normalize_code(profile.get("code"))
            if wanted and profile_name != wanted:
                continue
            for key, value in profile.items():
                resolved[key] = self._stringify(value)
            break
        return resolved

    def _load_route_rules(self, service_type: str) -> list[dict[str, object]]:
        if not self.settings.mysql_read_shared_config:
            return []
        sql = text(
            f"""
            SELECT route_rule_id, scene_code, template_code, content_mode, function_type,
                   provider_code, profile_code, match_json
            FROM {self.settings.third_party_route_table}
            WHERE service_type = :service_type AND is_enabled = 1
            ORDER BY priority ASC, route_rule_id ASC
            """
        )
        try:
            with get_engine().connect() as conn:
                rows = conn.execute(sql, {"service_type": service_type}).mappings().all()
        except Exception:
            return []
        return [dict(row) for row in rows]

    def _matches(self, rule: dict[str, object], route_context: dict[str, str]) -> bool:
        for key in ("scene_code", "template_code", "content_mode", "function_type"):
            expected = str(rule.get(key) or "").strip()
            if expected and route_context.get(key, "").strip().lower() != expected.lower():
                return False
        match_json = str(rule.get("match_json") or "").strip()
        if not match_json:
            return True
        try:
            extra = json.loads(match_json)
        except json.JSONDecodeError:
            return False
        for key, expected in extra.items():
            actual = route_context.get(key, "").strip().lower()
            if isinstance(expected, list):
                candidates = [str(item).strip().lower() for item in expected if item is not None]
                if actual not in candidates:
                    return False
                continue
            if actual != str(expected).strip().lower():
                return False
        return True

    def _normalize_code(self, value: Optional[object]) -> str:
        if value is None:
            return ""
        return str(value).strip().lower().replace("-", "_")

    def _stringify(self, value: object) -> str:
        if value is None:
            return ""
        if isinstance(value, (dict, list)):
            return json.dumps(value, ensure_ascii=True)
        return str(value)
