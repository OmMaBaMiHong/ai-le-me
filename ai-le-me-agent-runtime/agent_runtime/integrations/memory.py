from __future__ import annotations

from agent_runtime.core.config import Settings
from agent_runtime.integrations.base import IntegrationProfile


class MemoryCatalog:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def list_profiles(self) -> list[IntegrationProfile]:
        primary = self.settings.memory_primary.strip().lower()
        secondary = self.settings.memory_secondary.strip().lower()
        return [
            IntegrationProfile(
                code="mem0",
                name="Mem0",
                role="Primary memory extraction and recall layer for user preference facts and conversational memory.",
                selected=primary == "mem0" or secondary == "mem0",
                enabled=primary == "mem0",
                package_name="mem0ai",
                module_name="mem0",
                docs_url="https://github.com/mem0ai/mem0",
                notes=(
                    "Best used as the default fact-memory layer in front of the companion agent.",
                    "Good match for lightweight memory writes from chats, tags, and behavior signals.",
                ),
            ),
            IntegrationProfile(
                code="letta",
                name="Letta",
                role="Stateful agent memory and persona continuity for longer-running companion sessions.",
                selected=primary == "letta" or secondary == "letta",
                enabled=primary == "letta",
                package_name="letta-client",
                module_name="letta_client",
                docs_url="https://github.com/letta-ai/letta",
                notes=(
                    "Recommended as a secondary stateful memory plane for deep companion sessions.",
                    "Best reserved for richer owner-assistant continuity rather than every lightweight recall.",
                ),
            ),
        ]
