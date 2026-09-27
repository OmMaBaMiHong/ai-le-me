from __future__ import annotations

from typing import Optional

from agent_runtime.core.config import get_settings
from agent_runtime.repositories.runtime_config import ResolvedRoute, RuntimeConfigRepository
from agent_runtime.schemas.runtime import RuntimeConfigSnapshot, RuntimeResolvedRoute


class SharedConfigRouter:
    def __init__(self, repository: RuntimeConfigRepository) -> None:
        self.repository = repository
        self.settings = get_settings()

    def resolve_ai_route(self, scene_code: str, function_type: Optional[str] = None) -> ResolvedRoute:
        return self.repository.resolve_route(
            service_type=self.settings.default_ai_service_type,
            route_context={
                "scene_code": scene_code,
                "function_type": function_type or scene_code,
            },
            fallback_provider=self.settings.default_ai_provider,
        )

    def snapshot(self) -> RuntimeConfigSnapshot:
        route = self.resolve_ai_route(scene_code="companion.reply", function_type="companion_reply")
        return RuntimeConfigSnapshot(
            ai_route=RuntimeResolvedRoute(
                service_type=route.service_type,
                provider_code=route.provider_code,
                profile_code=route.profile_code,
                route_rule_id=route.route_rule_id,
                configs=route.configs,
            ),
            persona_provider=self.settings.default_persona_provider,
            companion_provider=self.settings.default_companion_provider,
        )
