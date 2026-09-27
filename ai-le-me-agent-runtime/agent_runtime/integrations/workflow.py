from __future__ import annotations

from agent_runtime.core.config import Settings
from agent_runtime.integrations.base import IntegrationProfile


class WorkflowCatalog:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def resolve(self) -> IntegrationProfile:
        for profile in self.list_profiles():
            if profile.selected:
                return profile
        profiles = self.list_profiles()
        return profiles[0]

    def list_profiles(self) -> list[IntegrationProfile]:
        selected = self.settings.workflow_engine.strip().lower()
        return [
            IntegrationProfile(
                code="langgraph",
                name="LangGraph",
                role="Multi-step orchestration, resumable workflows, human-in-the-loop approvals.",
                selected=selected == "langgraph",
                enabled=self.settings.workflow_enabled and selected == "langgraph",
                package_name="langgraph",
                module_name="langgraph",
                docs_url="https://github.com/langchain-ai/langgraph",
                notes=(
                    "Recommended as the main workflow engine for companion and hongniang flows.",
                    "Good fit for approval gates before high-risk social actions.",
                ),
            ),
        ]
