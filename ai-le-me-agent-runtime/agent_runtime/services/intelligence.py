from __future__ import annotations

import re
import uuid
from dataclasses import dataclass, field
from typing import Any, TypedDict

import httpx

from agent_runtime.core.config import Settings
from agent_runtime.schemas.companion import CompanionReplyRequest, CompanionStrategyRequest
from agent_runtime.schemas.match import SmartMatchRequest
from agent_runtime.schemas.persona import PersonaReportRequest, PersonaReportResponse
from agent_runtime.services.retrieval import LlamaIndexRetrievalService, RetrievedCandidate


@dataclass
class PersonaInsightContext:
    memory_hits: list[str] = field(default_factory=list)
    graph_facts: list[str] = field(default_factory=list)
    interest_hints: list[str] = field(default_factory=list)


@dataclass
class CompanionInsightContext:
    memory_hits: list[str] = field(default_factory=list)
    graph_facts: list[str] = field(default_factory=list)
    topic_hint: str | None = None


@dataclass
class MatchInsightContext:
    memory_hits: list[str] = field(default_factory=list)
    graph_facts: list[str] = field(default_factory=list)
    candidates: list[RetrievedCandidate] = field(default_factory=list)


class PersonaWorkflowState(TypedDict, total=False):
    query_text: str
    user_key: str
    memory_hits: list[str]
    graph_facts: list[str]
    interest_hints: list[str]


class CompanionWorkflowState(TypedDict, total=False):
    query_text: str
    user_key: str
    memory_hits: list[str]
    graph_facts: list[str]
    topic_hint: str


class OllamaEmbeddingClient:
    def __init__(self, settings: Settings) -> None:
        self.base_url = settings.ollama_base_url.rstrip("/")
        self.model = settings.ollama_embed_model
        self.timeout = httpx.Timeout(20.0, connect=5.0)

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        cleaned = [text.strip() for text in texts if text and text.strip()]
        if not cleaned:
            return []
        try:
            return self._embed_batch(cleaned)
        except Exception:
            return []

    def _embed_batch(self, texts: list[str]) -> list[list[float]]:
        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(
                f"{self.base_url}/api/embed",
                json={"model": self.model, "input": texts},
            )
            if response.is_success:
                data = response.json()
                embeddings = data.get("embeddings")
                if isinstance(embeddings, list) and embeddings and isinstance(embeddings[0], list):
                    return embeddings
                embedding = data.get("embedding")
                if isinstance(embedding, list) and embedding and isinstance(embedding[0], (int, float)):
                    return [embedding]
            fallback_embeddings: list[list[float]] = []
            for text in texts:
                fallback = client.post(
                    f"{self.base_url}/api/embeddings",
                    json={"model": self.model, "prompt": text},
                )
                if not fallback.is_success:
                    return []
                data = fallback.json()
                embedding = data.get("embedding")
                if not isinstance(embedding, list):
                    return []
                fallback_embeddings.append(embedding)
            return fallback_embeddings


class VectorMemoryStore:
    def __init__(self, settings: Settings, embeddings: OllamaEmbeddingClient) -> None:
        self.settings = settings
        self.embeddings = embeddings
        self._client: Any | None = None
        self._splitter: Any | None = None
        self._rest_base = f"http://{settings.chroma_host}:{settings.chroma_port}/api/v2"
        self._tenant = "default_tenant"
        self._database = "default_database"
        try:
            from llama_index.core.node_parser import SentenceSplitter

            self._splitter = SentenceSplitter(chunk_size=360, chunk_overlap=48)
        except Exception:
            self._splitter = None
        if not settings.local_vector_enabled:
            return
        try:
            import chromadb

            self._client = chromadb.HttpClient(host=settings.chroma_host, port=settings.chroma_port)
        except Exception:
            self._client = None

    def is_available(self) -> bool:
        if self._client is not None:
            return True
        try:
            response = httpx.get(f"{self._rest_base}/heartbeat", timeout=3.0)
            return response.is_success
        except Exception:
            return False

    def search(self, namespace: str, user_key: str, query_text: str, top_k: int = 4) -> list[str]:
        if not query_text.strip():
            return []
        query_embeddings = self.embeddings.embed_texts([query_text])
        if not query_embeddings:
            return []
        if self._client is not None:
            collection = self._get_collection(namespace, user_key)
            if collection is None:
                return []
            try:
                result = collection.query(
                    query_embeddings=query_embeddings,
                    n_results=max(1, top_k),
                    include=["documents"],
                )
            except Exception:
                return []
            documents = result.get("documents") or []
            if not documents:
                return []
            return [str(item) for item in documents[0] if item]
        collection_id = self._get_or_create_collection_id(namespace, user_key)
        if not collection_id:
            return []
        try:
            with httpx.Client(timeout=10.0) as client:
                result = client.post(
                    f"{self._collection_url(collection_id)}/query",
                    json={
                        "query_embeddings": query_embeddings,
                        "n_results": max(1, top_k),
                        "include": ["documents"],
                    },
                )
                result.raise_for_status()
                data = result.json()
        except Exception:
            return []
        documents = data.get("documents") or []
        if not documents:
            return []
        return [str(item) for item in documents[0] if item]

    def store(self, namespace: str, user_key: str, texts: list[str], metadata: dict[str, Any] | None = None) -> int:
        entries = self._prepare_entries(texts, metadata or {})
        if not entries:
            return 0
        documents = [entry["text"] for entry in entries]
        embeddings = self.embeddings.embed_texts(documents)
        if len(embeddings) != len(documents):
            return 0
        ids = [uuid.uuid4().hex for _ in documents]
        metadatas = [entry["metadata"] for entry in entries]
        if self._client is not None:
            collection = self._get_collection(namespace, user_key)
            if collection is None:
                return 0
            try:
                collection.add(
                    ids=ids,
                    documents=documents,
                    embeddings=embeddings,
                    metadatas=metadatas,
                )
            except Exception:
                return 0
            return len(documents)
        collection_id = self._get_or_create_collection_id(namespace, user_key)
        if not collection_id:
            return 0
        try:
            with httpx.Client(timeout=10.0) as client:
                result = client.post(
                    f"{self._collection_url(collection_id)}/upsert",
                    json={
                        "ids": ids,
                        "documents": documents,
                        "embeddings": embeddings,
                        "metadatas": metadatas,
                    },
                )
                result.raise_for_status()
        except Exception:
            return 0
        return len(documents)

    def _prepare_entries(self, texts: list[str], metadata: dict[str, Any]) -> list[dict[str, Any]]:
        cleaned = self._prepare_chunks(texts)
        if not cleaned:
            return []
        entries: list[dict[str, Any]] = []
        seen: set[str] = set()
        for text in cleaned:
            normalized = text.strip()
            if not normalized or normalized in seen:
                continue
            seen.add(normalized)
            entries.append({"text": normalized, "metadata": dict(metadata or {}, variant="raw")})
            for hypothetical in self._build_hypothetical_entries(normalized, metadata):
                if hypothetical in seen:
                    continue
                seen.add(hypothetical)
                entries.append({"text": hypothetical, "metadata": dict(metadata or {}, variant="hypothetical")})
        return entries

    def _build_hypothetical_entries(self, text: str, metadata: dict[str, Any]) -> list[str]:
        if not getattr(self.settings, "retrieval_hypothetical_enabled", True):
            return []
        subject = str(metadata.get("subject_nickname") or "这个对象").strip()
        kind = str(metadata.get("kind") or "").strip()
        prompts: list[str] = []
        if kind in {"persona_report", "persona_profile", "persona_pair_memory"}:
            prompts.extend(
                [
                    f"问：{subject}的性格、关系风格和核心结论是什么？\n答：{text}",
                    f"问：如果想接近{subject}，应该注意什么、怎么开场？\n答：{text}",
                ]
            )
        elif kind in {"reply_suggestion", "reply_pair_memory", "next_step", "strategy_pair_memory", "gift_plan", "gift_pair_memory"}:
            prompts.extend(
                [
                    f"问：面对{subject}，下一步该怎么聊、怎么推进？\n答：{text}",
                    f"问：如果要继续和{subject}互动，什么表达最合适？\n答：{text}",
                ]
            )
        elif kind == "smart_match_snapshot":
            prompts.extend(
                [
                    f"问：为什么推荐这些对象给{subject}？\n答：{text}",
                    f"问：如果和推荐对象破冰，最合适的切口是什么？\n答：{text}",
                ]
            )
        else:
            prompts.extend(
                [
                    f"问：关于{subject}最重要的信息是什么？\n答：{text}",
                    f"问：如果围绕{subject}做检索，最可能返回什么答案？\n答：{text}",
                ]
            )
        limit = max(0, int(getattr(self.settings, "retrieval_hypothetical_per_text", 2) or 2))
        return prompts[:limit]

    def _prepare_chunks(self, texts: list[str]) -> list[str]:
        cleaned = [text.strip() for text in texts if text and text.strip()]
        if not cleaned:
            return []
        if self._splitter is None:
            return cleaned
        try:
            chunks: list[str] = []
            for text in cleaned:
                nodes = self._splitter.get_nodes_from_documents([self._build_document(text)])
                for node in nodes:
                    chunk = str(getattr(node, "text", "") or "").strip()
                    if chunk:
                        chunks.append(chunk)
            return chunks or cleaned
        except Exception:
            return cleaned

    def _build_document(self, text: str):
        try:
            from llama_index.core.schema import Document

            return Document(text=text)
        except Exception:
            return type("Doc", (), {"text": text})()

    def _get_collection(self, namespace: str, user_key: str):
        if not self._client:
            return None
        name = self._collection_name(namespace, user_key)
        try:
            return self._client.get_or_create_collection(name=name)
        except Exception:
            return None

    def _collection_name(self, namespace: str, user_key: str) -> str:
        base = f"{self.settings.chroma_collection_prefix}_{namespace}_{user_key}"
        normalized = re.sub(r"[^a-zA-Z0-9_-]+", "_", base).strip("_").lower()
        return normalized[:60]

    def _collection_url(self, collection_id: str) -> str:
        return (
            f"{self._rest_base}/tenants/{self._tenant}/databases/"
            f"{self._database}/collections/{collection_id}"
        )

    def _get_or_create_collection_id(self, namespace: str, user_key: str) -> str | None:
        name = self._collection_name(namespace, user_key)
        base_url = (
            f"{self._rest_base}/tenants/{self._tenant}/databases/"
            f"{self._database}/collections"
        )
        try:
            with httpx.Client(timeout=10.0) as client:
                listing = client.get(base_url)
                listing.raise_for_status()
                for item in listing.json():
                    if item.get("name") == name and item.get("id"):
                        return str(item["id"])
                created = client.post(
                    base_url,
                    json={"name": name, "get_or_create": True},
                )
                created.raise_for_status()
                data = created.json()
                if data.get("id"):
                    return str(data["id"])
        except Exception:
            return None
        return None


class RelationshipGraphStore:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._driver: Any | None = None
        self._available: bool | None = None
        if not settings.local_graph_enabled:
            return
        try:
            from neo4j import GraphDatabase

            self._driver = GraphDatabase.driver(
                settings.neo4j_uri,
                auth=(settings.neo4j_user, settings.neo4j_password),
            )
        except Exception:
            self._driver = None

    def is_available(self) -> bool:
        if self._driver is None:
            return False
        if self._available is not None:
            return self._available
        try:
            self._driver.verify_connectivity()
            self._available = True
        except Exception:
            self._available = False
        return self._available

    def record_relationship(
        self,
        owner_id: int | None,
        target_id: int | None,
        owner_tags: list[str],
        target_tags: list[str],
        relationship_tags: list[str],
        message_count: int,
    ) -> None:
        if not self.is_available() or owner_id is None or target_id is None:
            return
        query = """
        MERGE (owner:User {user_id: $owner_id})
        MERGE (target:User {user_id: $target_id})
        MERGE (owner)-[r:INTERACTED_WITH]->(target)
        SET r.message_count = $message_count,
            r.relationship_tags = $relationship_tags,
            r.updated_at = datetime()
        WITH owner, target
        UNWIND $owner_tags AS owner_tag
        MERGE (ot:Tag {name: owner_tag})
        MERGE (owner)-[:HAS_TAG]->(ot)
        WITH target
        UNWIND $target_tags AS target_tag
        MERGE (tt:Tag {name: target_tag})
        MERGE (target)-[:HAS_TAG]->(tt)
        """
        self._execute_write(
            query,
            {
                "owner_id": owner_id,
                "target_id": target_id,
                "owner_tags": owner_tags,
                "target_tags": target_tags,
                "relationship_tags": relationship_tags,
                "message_count": message_count,
            },
        )

    def describe_relationship(self, owner_id: int | None, target_id: int | None) -> list[str]:
        if not self.is_available() or owner_id is None or target_id is None:
            return []
        query = """
        MATCH (owner:User {user_id: $owner_id})-[r:INTERACTED_WITH]->(target:User {user_id: $target_id})
        OPTIONAL MATCH (owner)-[:HAS_TAG]->(t:Tag)<-[:HAS_TAG]-(target)
        RETURN coalesce(r.message_count, 0) AS message_count,
               coalesce(r.relationship_tags, []) AS relationship_tags,
               collect(DISTINCT t.name)[0..4] AS shared_tags
        """
        if not self._driver:
            return []
        try:
            with self._driver.session(database=self.settings.neo4j_database) as session:
                row = session.run(query, owner_id=owner_id, target_id=target_id).single()
        except Exception:
            return []
        if row is None:
            return []
        facts: list[str] = []
        message_count = int(row.get("message_count") or 0)
        if message_count > 0:
            facts.append(f"图谱记录显示双方已有 {message_count} 条有效互动样本")
        relationship_tags = [str(tag) for tag in (row.get("relationship_tags") or []) if tag]
        if relationship_tags:
            facts.append(f"图谱关系标签: {', '.join(relationship_tags[:3])}")
        shared_tags = [str(tag) for tag in (row.get("shared_tags") or []) if tag]
        if shared_tags:
            facts.append(f"图谱共同兴趣: {', '.join(shared_tags[:3])}")
        return facts

    def _execute_write(self, query: str, parameters: dict[str, Any]) -> None:
        if not self._driver:
            return
        try:
            with self._driver.session(database=self.settings.neo4j_database) as session:
                session.run(query, **parameters)
        except Exception:
            return


class WorkflowRuntime:
    def __init__(
        self,
        settings: Settings,
        memory_store: VectorMemoryStore,
        graph_store: RelationshipGraphStore,
        retrieval: LlamaIndexRetrievalService,
    ) -> None:
        self.settings = settings
        self.memory_store = memory_store
        self.graph_store = graph_store
        self.retrieval = retrieval
        self._langgraph_available = False
        self._persona_graph: Any | None = None
        self._companion_graph: Any | None = None
        if settings.workflow_enabled:
            try:
                from langgraph.graph import END, START, StateGraph

                self._langgraph_available = True

                persona_graph = StateGraph(PersonaWorkflowState)
                persona_graph.add_node("memory_recall", self._persona_memory_node)
                persona_graph.add_node("graph_lookup", self._persona_graph_node)
                persona_graph.add_node("derive_interests", self._persona_interest_node)
                persona_graph.add_edge(START, "memory_recall")
                persona_graph.add_edge("memory_recall", "graph_lookup")
                persona_graph.add_edge("graph_lookup", "derive_interests")
                persona_graph.add_edge("derive_interests", END)
                self._persona_graph = persona_graph.compile()

                companion_graph = StateGraph(CompanionWorkflowState)
                companion_graph.add_node("memory_recall", self._companion_memory_node)
                companion_graph.add_node("graph_lookup", self._companion_graph_node)
                companion_graph.add_node("topic_choice", self._companion_topic_node)
                companion_graph.add_edge(START, "memory_recall")
                companion_graph.add_edge("memory_recall", "graph_lookup")
                companion_graph.add_edge("graph_lookup", "topic_choice")
                companion_graph.add_edge("topic_choice", END)
                self._companion_graph = companion_graph.compile()
            except Exception:
                self._langgraph_available = False

    def build_persona_context(self, payload: PersonaReportRequest) -> PersonaInsightContext:
        state: PersonaWorkflowState = {
            "query_text": self._persona_query(payload),
            "user_key": self._pair_key(payload.owner.user_id if payload.owner else None, payload.target.user_id),
        }
        if self._langgraph_available and self._persona_graph is not None:
            result = self._persona_graph.invoke(state)
        else:
            result = self._persona_interest_node(self._persona_graph_node(self._persona_memory_node(state)))
        return PersonaInsightContext(
            memory_hits=result.get("memory_hits", []),
            graph_facts=result.get("graph_facts", []),
            interest_hints=result.get("interest_hints", []),
        )

    def build_companion_context(self, payload: CompanionReplyRequest | CompanionStrategyRequest) -> CompanionInsightContext:
        state: CompanionWorkflowState = {
            "query_text": self._companion_query(payload),
            "user_key": self._pair_key(payload.owner.user_id, payload.target.user_id),
        }
        if self._langgraph_available and self._companion_graph is not None:
            result = self._companion_graph.invoke(state)
        else:
            result = self._companion_topic_node(self._companion_graph_node(self._companion_memory_node(state)))
        return CompanionInsightContext(
            memory_hits=result.get("memory_hits", []),
            graph_facts=result.get("graph_facts", []),
            topic_hint=result.get("topic_hint"),
        )

    def _persona_memory_node(self, state: PersonaWorkflowState) -> PersonaWorkflowState:
        hits = self.retrieval.retrieve_memory("persona", state["user_key"], state["query_text"])
        return {**state, "memory_hits": hits}

    def _persona_graph_node(self, state: PersonaWorkflowState) -> PersonaWorkflowState:
        owner_id, target_id = self._split_pair_key(state["user_key"])
        facts = self.graph_store.describe_relationship(owner_id, target_id)
        return {**state, "graph_facts": facts}

    def _persona_interest_node(self, state: PersonaWorkflowState) -> PersonaWorkflowState:
        hints: list[str] = []
        for item in state.get("memory_hits", []) + state.get("graph_facts", []):
            parts = re.split(r"[,:，、]", item)
            for part in parts:
                text = part.strip()
                if 1 < len(text) <= 12 and text not in hints:
                    hints.append(text)
        return {**state, "interest_hints": hints[:4]}

    def _companion_memory_node(self, state: CompanionWorkflowState) -> CompanionWorkflowState:
        hits = self.retrieval.retrieve_memory("companion", state["user_key"], state["query_text"])
        return {**state, "memory_hits": hits}

    def _companion_graph_node(self, state: CompanionWorkflowState) -> CompanionWorkflowState:
        owner_id, target_id = self._split_pair_key(state["user_key"])
        facts = self.graph_store.describe_relationship(owner_id, target_id)
        return {**state, "graph_facts": facts}

    def _companion_topic_node(self, state: CompanionWorkflowState) -> CompanionWorkflowState:
        topic = ""
        for item in state.get("memory_hits", []) + state.get("graph_facts", []):
            match = re.search(r"[:：]\s*([^,，。；;]+)", item)
            if match:
                topic = match.group(1).strip()
                break
        return {**state, "topic_hint": topic}

    def _pair_key(self, owner_id: int | None, target_id: int | None) -> str:
        return f"{owner_id or 0}_{target_id or 0}"

    def _split_pair_key(self, key: str) -> tuple[int | None, int | None]:
        left, _, right = key.partition("_")
        try:
            return int(left), int(right)
        except ValueError:
            return None, None

    def _persona_query(self, payload: PersonaReportRequest) -> str:
        parts = [
            payload.target.nickname or "",
            " ".join(payload.target.tags),
            " ".join(payload.relationship_tags),
            " ".join(message.content for message in payload.recent_messages[-4:]),
        ]
        return " ".join(item for item in parts if item).strip()

    def _companion_query(self, payload: CompanionReplyRequest | CompanionStrategyRequest) -> str:
        parts = [
            payload.target.nickname or "",
            " ".join(payload.target.tags),
            " ".join(payload.memory.known_preferences),
            " ".join(message.content for message in payload.recent_messages[-4:]),
        ]
        return " ".join(item for item in parts if item).strip()


class AgentIntelligenceService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.embeddings = OllamaEmbeddingClient(settings)
        self.memory_store = VectorMemoryStore(settings, self.embeddings)
        self.graph_store = RelationshipGraphStore(settings)
        self.retrieval = LlamaIndexRetrievalService(self.embeddings, self.memory_store)
        self.workflow = WorkflowRuntime(settings, self.memory_store, self.graph_store, self.retrieval)

    def build_persona_context(self, payload: PersonaReportRequest) -> PersonaInsightContext:
        context = self.workflow.build_persona_context(payload)
        pair_key = f"{payload.owner.user_id if payload.owner else 0}_{payload.target.user_id or 0}"
        query_text = self.workflow._persona_query(payload)
        hits = self._retrieve_multi_namespace(
            [
                ("persona", pair_key),
                ("persona_profile", str(payload.target.user_id or 0)),
                ("pair_memory", pair_key),
            ],
            query_text,
            top_k=4,
        )
        context.memory_hits = hits[:4]
        return context

    def build_companion_context(self, payload: CompanionReplyRequest | CompanionStrategyRequest) -> CompanionInsightContext:
        context = self.workflow.build_companion_context(payload)
        pair_key = f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}"
        query_text = self.workflow._companion_query(payload)
        hits = self._retrieve_multi_namespace(
            [
                ("companion", pair_key),
                ("pair_memory", pair_key),
                ("strategy", pair_key),
                ("gift_plan", pair_key),
                ("persona_profile", str(payload.target.user_id or 0)),
            ],
            query_text,
            top_k=4,
        )
        context.memory_hits = hits[:4]
        return context

    def build_match_context(self, payload: SmartMatchRequest) -> MatchInsightContext:
        owner_id = payload.owner.user_id or 0
        user_key = f"{owner_id}_{owner_id}"
        query_text = self.retrieval.build_match_query(payload.owner, payload.preference)
        memory_hits = self._retrieve_multi_namespace(
            [
                ("persona", user_key),
                ("persona_profile", str(owner_id)),
                ("match_history", str(owner_id)),
            ],
            query_text,
            top_k=4,
        )
        candidates = self.retrieval.retrieve_candidates(
            owner=payload.owner,
            preference=payload.preference,
            candidates=payload.candidates,
            top_k=min(max(payload.limit * 3, 6), 12),
        )
        graph_facts: list[str] = []
        for retrieved in candidates[:4]:
            facts = self.graph_store.describe_relationship(owner_id, retrieved.candidate.user_id)
            for fact in facts[:2]:
                if fact not in graph_facts:
                    graph_facts.append(fact)
                if len(graph_facts) >= 6:
                    break
            if len(graph_facts) >= 6:
                break
        return MatchInsightContext(memory_hits=memory_hits, graph_facts=graph_facts, candidates=candidates)

    def record_persona(self, payload: PersonaReportRequest, response: PersonaReportResponse) -> None:
        owner_id = payload.owner.user_id if payload.owner else None
        target_id = payload.target.user_id
        self.graph_store.record_relationship(
            owner_id=owner_id,
            target_id=target_id,
            owner_tags=payload.owner.tags if payload.owner else [],
            target_tags=payload.target.tags,
            relationship_tags=payload.relationship_tags,
            message_count=len(payload.recent_messages),
        )
        kernel = response.persona_kernel
        texts = [
            response.summary,
            kernel.expression_style,
            kernel.relationship_style,
            f"爱的语言: {'、'.join(kernel.love_language[:3])}",
            f"恋爱节奏: {kernel.romance_pace}",
            f"适配主风格: {kernel.preferred_master_style_code or ''} {kernel.style_router_reason or ''}".strip(),
        ]
        texts.extend(response.evidence_digest[:3])
        texts.extend(response.interest_clusters[:3])
        texts.extend([f"稳定特质 {item.name}: {item.reason}" for item in kernel.stable_traits[:3]])
        texts.extend([f"推进雷区: {item}" for item in kernel.taboo_rules[:2]])
        texts.extend([f"风格候选 {item.style_code}: {item.reason}" for item in kernel.coach_style_candidates[:2]])
        self.memory_store.store(
            "persona",
            f"{owner_id or 0}_{target_id or 0}",
            texts,
            metadata={
                "kind": "persona_report",
                "goal": payload.report_goal or "dating_companion",
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": target_id or 0,
            },
        )
        self.memory_store.store(
            "persona_profile",
            str(target_id or 0),
            texts + response.approach_suggestions[:2] + response.risk_flags[:2] + kernel.taboo_rules[:2],
            metadata={
                "kind": "persona_profile",
                "goal": payload.report_goal or "dating_companion",
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": target_id or 0,
            },
        )
        if owner_id is not None:
            self.memory_store.store(
                "pair_memory",
                f"{owner_id}_{target_id or 0}",
                texts[:4] + response.approach_suggestions[:2] + kernel.taboo_rules[:1],
                metadata={
                    "kind": "persona_pair_memory",
                    "goal": payload.report_goal or "dating_companion",
                    "subject_nickname": payload.target.nickname or "",
                    "subject_user_id": target_id or 0,
                    "owner_user_id": owner_id,
                },
            )

    def record_companion_reply(self, payload: CompanionReplyRequest, response_texts: list[str]) -> None:
        self.graph_store.record_relationship(
            owner_id=payload.owner.user_id,
            target_id=payload.target.user_id,
            owner_tags=payload.owner.tags,
            target_tags=payload.target.tags,
            relationship_tags=[],
            message_count=len(payload.recent_messages),
        )
        self.memory_store.store(
            "companion",
            f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}",
            response_texts + payload.memory.known_preferences[:2],
            metadata={
                "kind": "reply_suggestion",
                "goal": payload.owner_goal,
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": payload.target.user_id or 0,
            },
        )
        self.memory_store.store(
            "pair_memory",
            f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}",
            response_texts[:2] + payload.memory.known_preferences[:2],
            metadata={
                "kind": "reply_pair_memory",
                "goal": payload.owner_goal,
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": payload.target.user_id or 0,
                "owner_user_id": payload.owner.user_id or 0,
            },
        )

    def record_companion_strategy(self, payload: CompanionStrategyRequest, next_action: str, plan: list[str]) -> None:
        self.graph_store.record_relationship(
            owner_id=payload.owner.user_id,
            target_id=payload.target.user_id,
            owner_tags=payload.owner.tags,
            target_tags=payload.target.tags,
            relationship_tags=[],
            message_count=len(payload.recent_messages),
        )
        self.memory_store.store(
            "strategy",
            f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}",
            [next_action] + plan[:2],
            metadata={
                "kind": "next_step",
                "objective": payload.objective,
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": payload.target.user_id or 0,
            },
        )
        self.memory_store.store(
            "pair_memory",
            f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}",
            [next_action] + plan[:2],
            metadata={
                "kind": "strategy_pair_memory",
                "objective": payload.objective,
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": payload.target.user_id or 0,
                "owner_user_id": payload.owner.user_id or 0,
            },
        )

    def record_companion_gift_plan(self, payload, drafts: list[str], strategy_note: str) -> None:
        self.graph_store.record_relationship(
            owner_id=payload.owner.user_id,
            target_id=payload.target.user_id,
            owner_tags=payload.owner.tags,
            target_tags=payload.target.tags,
            relationship_tags=[],
            message_count=len(payload.recent_messages),
        )
        self.memory_store.store(
            "gift_plan",
            f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}",
            [strategy_note] + drafts[:2],
            metadata={
                "kind": "gift_plan",
                "objective": payload.objective,
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": payload.target.user_id or 0,
            },
        )
        self.memory_store.store(
            "pair_memory",
            f"{payload.owner.user_id or 0}_{payload.target.user_id or 0}",
            [strategy_note] + drafts[:2],
            metadata={
                "kind": "gift_pair_memory",
                "objective": payload.objective,
                "subject_nickname": payload.target.nickname or "",
                "subject_user_id": payload.target.user_id or 0,
                "owner_user_id": payload.owner.user_id or 0,
            },
        )

    def record_match_recommendation(self, payload: SmartMatchRequest, matches) -> None:
        owner_id = payload.owner.user_id or 0
        texts: list[str] = []
        for item in matches[:5]:
            reasons = getattr(item, "reasons", []) or []
            openers = getattr(item, "icebreak_openers", []) or []
            texts.append(
                "；".join(
                    part
                    for part in [
                        f"user_id={getattr(item, 'user_id', 0)}",
                        f"score={getattr(item, 'score', 0)}",
                        f"summary={getattr(item, 'summary', '')}",
                        f"reasons={','.join(reasons[:2])}",
                        f"openers={','.join(openers[:1])}",
                    ]
                    if part
                )
            )
        if not texts:
            return
        self.memory_store.store(
            "match_history",
            str(owner_id),
            texts,
            metadata={
                "kind": "smart_match_snapshot",
                "limit": payload.limit,
                "subject_nickname": payload.owner.nickname or "",
                "subject_user_id": owner_id,
            },
        )

    def _retrieve_multi_namespace(
        self,
        lookups: list[tuple[str, str]],
        query_text: str,
        top_k: int,
    ) -> list[str]:
        merged: list[str] = []
        seen: set[str] = set()
        for namespace, user_key in lookups:
            if not namespace or not user_key:
                continue
            hits = self.retrieval.retrieve_memory(namespace, user_key, query_text, top_k=top_k)
            for hit in hits:
                if not hit or hit in seen:
                    continue
                merged.append(hit)
                seen.add(hit)
                if len(merged) >= top_k:
                    return merged
        return merged
