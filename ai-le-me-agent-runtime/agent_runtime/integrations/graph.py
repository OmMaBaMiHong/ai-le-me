from __future__ import annotations

from agent_runtime.core.config import Settings
from agent_runtime.integrations.base import IntegrationProfile


class GraphCatalog:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def list_profiles(self) -> list[IntegrationProfile]:
        primary = self.settings.graph_primary.strip().lower()
        secondary = self.settings.graph_secondary.strip().lower()
        return [
            IntegrationProfile(
                code="graphiti",
                name="Graphiti",
                role="Temporal relationship graph for evolving people-topic-intent edges.",
                selected=primary == "graphiti" or secondary == "graphiti",
                enabled=primary == "graphiti",
                package_name="graphiti-core",
                module_name="graphiti_core",
                docs_url="https://github.com/getzep/graphiti",
                notes=(
                    "Recommended for social relationship evolution, timing, and stage-aware memory.",
                    "Good fit for hongniang and companion scenarios where relationships change over time.",
                ),
            ),
            IntegrationProfile(
                code="neo4j",
                name="Neo4j",
                role="LPG storage and graph query engine for recommendation, explainability, and risk analysis.",
                selected=primary == "neo4j" or secondary == "neo4j",
                enabled=primary == "neo4j",
                package_name="neo4j",
                module_name="neo4j",
                docs_url="https://neo4j.com/use-cases/real-time-recommendation-engine/",
                notes=(
                    "Recommended as the production graph query layer once recommendation and explainability grow.",
                    "Strong fit for matchmaking, social-path recommendation, and fraud/risk link analysis.",
                ),
            ),
        ]
