from __future__ import annotations

import math
import re
from dataclasses import dataclass, field
from typing import Any

from agent_runtime.schemas.common import UserCard
from agent_runtime.schemas.match import MatchCandidate, MatchPreference


@dataclass
class RetrievedCandidate:
    candidate: MatchCandidate
    score: float
    evidence: list[str] = field(default_factory=list)


class LlamaIndexRetrievalService:
    def __init__(self, embeddings, memory_store) -> None:
        self.embeddings = embeddings
        self.memory_store = memory_store
        self.settings = getattr(memory_store, "settings", None)
        self._splitter: Any | None = None
        self._document_cls: Any | None = None
        self._query_bundle_cls: Any | None = None
        try:
            from llama_index.core.node_parser import SentenceSplitter
            from llama_index.core.schema import Document, QueryBundle

            self._splitter = SentenceSplitter(chunk_size=320, chunk_overlap=48)
            self._document_cls = Document
            self._query_bundle_cls = QueryBundle
        except Exception:
            self._splitter = None
            self._document_cls = None
            self._query_bundle_cls = None

    def retrieve_memory(self, namespace: str, user_key: str, query_text: str, top_k: int = 4) -> list[str]:
        query = self._normalize_space(query_text)
        if not query:
            return []
        bundle = self._build_query_bundle(query)
        fetch_multiplier = max(1, int(getattr(self.settings, "retrieval_memory_fetch_multiplier", 3) or 3))
        threshold = float(getattr(self.settings, "retrieval_memory_score_threshold", 0.12) or 0.12)
        raw_hits = self.memory_store.search(namespace, user_key, bundle.query_str, top_k=max(top_k * fetch_multiplier, top_k))
        if not raw_hits:
            return []
        ranked: list[tuple[float, str]] = []
        total_hits = max(len(raw_hits), 1)
        for index, hit in enumerate(raw_hits):
            normalized_hit = self._normalize_memory_hit(hit)
            if not normalized_hit:
                continue
            lexical_score = max(
                self._score_text_hit(bundle.query_str, hit),
                self._score_text_hit(bundle.query_str, normalized_hit),
            )
            vector_order_score = max(0.0, 1 - (index / total_hits))
            ranked.append((round(lexical_score * 0.7 + vector_order_score * 0.3, 4), normalized_hit))
        ranked.sort(key=lambda item: item[0], reverse=True)
        ordered: list[str] = []
        for score, hit in ranked:
            if score < threshold:
                continue
            if hit not in ordered:
                ordered.append(hit)
            if len(ordered) >= top_k:
                break
        return ordered

    def retrieve_candidates(
        self,
        owner: UserCard,
        preference: MatchPreference,
        candidates: list[MatchCandidate],
        top_k: int,
    ) -> list[RetrievedCandidate]:
        if not candidates:
            return []
        query = self.build_match_query(owner, preference)
        bundle = self._build_query_bundle(query)
        nodes = self._build_candidate_nodes(candidates)
        query_embedding = self._get_query_embedding(bundle.query_str)
        node_texts = [node["text"] for node in nodes]
        node_embeddings = self.embeddings.embed_texts(node_texts) if query_embedding and node_texts else []

        candidate_scores: dict[int, float] = {}
        candidate_evidence: dict[int, list[str]] = {}
        for index, node in enumerate(nodes):
            candidate = node["candidate"]
            semantic_score = 0.0
            if query_embedding and index < len(node_embeddings):
                semantic_score = self._cosine_similarity(query_embedding, node_embeddings[index])
            lexical_score = self._score_text_hit(bundle.query_str, node["text"])
            preference_score, preference_evidence = self._preference_score(owner, preference, candidate)
            total_score = semantic_score * 0.6 + lexical_score * 0.25 + preference_score * 0.15
            current = candidate_scores.get(candidate.user_id)
            if current is None or total_score > current:
                candidate_scores[candidate.user_id] = total_score
                candidate_evidence[candidate.user_id] = preference_evidence or node["evidence"]
        threshold = float(getattr(self.settings, "retrieval_candidate_score_threshold", 0.1) or 0.1)
        ranked = sorted(
            (
                RetrievedCandidate(
                    candidate=candidate,
                    score=candidate_scores.get(candidate.user_id, 0.0),
                    evidence=(candidate_evidence.get(candidate.user_id) or [])[:3],
                )
                for candidate in candidates
                if candidate_scores.get(candidate.user_id, 0.0) >= threshold
            ),
            key=lambda item: item.score,
            reverse=True,
        )
        return ranked[: max(1, top_k)] if ranked else []

    def build_match_query(self, owner: UserCard, preference: MatchPreference) -> str:
        parts = [
            owner.nickname or "",
            owner.city or "",
            owner.summary or "",
            " ".join(owner.tags),
            " ".join(preference.preferred_cities),
        ]
        if preference.age_min is not None or preference.age_max is not None:
            parts.append(f"年龄 {preference.age_min or ''}-{preference.age_max or ''}")
        if preference.height_min is not None or preference.height_max is not None:
            parts.append(f"身高 {preference.height_min or ''}-{preference.height_max or ''}")
        if preference.education_levels:
            parts.append("学历 " + " ".join(str(level) for level in preference.education_levels))
        return self._normalize_space(" ".join(parts))

    def _build_query_bundle(self, query_text: str):
        if self._query_bundle_cls is not None:
            try:
                return self._query_bundle_cls(query_str=query_text)
            except Exception:
                pass
        return type("QueryBundleProxy", (), {"query_str": query_text})()

    def _build_candidate_nodes(self, candidates: list[MatchCandidate]) -> list[dict[str, Any]]:
        nodes: list[dict[str, Any]] = []
        for candidate in candidates:
            text = self._candidate_text(candidate)
            chunks = self._split_text(text)
            evidence = self._candidate_evidence(candidate)
            for chunk in chunks:
                nodes.append(
                    {
                        "candidate": candidate,
                        "text": chunk,
                        "evidence": evidence,
                    }
                )
            for chunk in self._candidate_hypothetical_texts(candidate, text):
                nodes.append(
                    {
                        "candidate": candidate,
                        "text": chunk,
                        "evidence": evidence,
                    }
                )
        return nodes

    def _split_text(self, text: str) -> list[str]:
        normalized = self._normalize_space(text)
        if not normalized:
            return []
        if self._splitter is None or self._document_cls is None:
            return [normalized]
        try:
            document = self._document_cls(text=normalized)
            chunks: list[str] = []
            for node in self._splitter.get_nodes_from_documents([document]):
                chunk = self._normalize_space(getattr(node, "text", ""))
                if chunk:
                    chunks.append(chunk)
            return chunks or [normalized]
        except Exception:
            return [normalized]

    def _candidate_text(self, candidate: MatchCandidate) -> str:
        parts = [
            candidate.nickname or "",
            candidate.city or "",
            f"{candidate.age or ''}岁" if candidate.age else "",
            f"{candidate.height or ''}cm" if candidate.height else "",
            f"学历{candidate.education}" if candidate.education is not None else "",
            candidate.job or "",
            candidate.summary or "",
            " ".join(candidate.tags),
            " ".join(candidate.interests),
        ]
        if candidate.verified:
            parts.append("资料认证")
        if candidate.likes_owner:
            parts.append("对你有好感")
        if candidate.liked_by_owner:
            parts.append("你已关注")
        return self._normalize_space("，".join(part for part in parts if part))

    def _candidate_evidence(self, candidate: MatchCandidate) -> list[str]:
        evidence: list[str] = []
        if candidate.city:
            evidence.append(f"候选城市: {candidate.city}")
        if candidate.tags:
            evidence.append(f"候选标签: {', '.join(candidate.tags[:3])}")
        if candidate.interests:
            evidence.append(f"兴趣片段: {', '.join(candidate.interests[:3])}")
        if candidate.likes_owner:
            evidence.append("关系信号: 对方已对你有关注")
        return evidence[:3]

    def _candidate_hypothetical_texts(self, candidate: MatchCandidate, base_text: str) -> list[str]:
        if not getattr(self.settings, "retrieval_hypothetical_enabled", True):
            return []
        nickname = candidate.nickname or "这个对象"
        prompts = [
            f"问：{nickname}适合什么样的人，为什么值得推荐？\n答：{base_text}",
            f"问：和{nickname}破冰时应该从什么话题切入？\n答：{base_text}",
        ]
        limit = max(0, int(getattr(self.settings, "retrieval_hypothetical_per_text", 2) or 2))
        return prompts[:limit]

    def _get_query_embedding(self, query_text: str) -> list[float]:
        embeddings = self.embeddings.embed_texts([query_text])
        if embeddings:
            return embeddings[0]
        return []

    def _score_text_hit(self, query_text: str, text: str) -> float:
        query_tokens = self._tokenize(query_text)
        text_tokens = self._tokenize(text)
        if not query_tokens or not text_tokens:
            return 0.0
        overlap = len(query_tokens & text_tokens)
        coverage = overlap / max(len(query_tokens), 1)
        density = overlap / max(len(text_tokens), 1)
        return round(coverage * 0.7 + density * 0.3, 4)

    def _normalize_memory_hit(self, text: str) -> str:
        normalized = self._normalize_space(text)
        if not normalized:
            return ""
        for marker in ("答：", "A:", "answer:"):
            if marker in normalized:
                answer = normalized.split(marker, 1)[1].strip()
                if answer:
                    return answer
        return normalized

    def _preference_score(
        self,
        owner: UserCard,
        preference: MatchPreference,
        candidate: MatchCandidate,
    ) -> tuple[float, list[str]]:
        score = 0.0
        evidence: list[str] = []
        owner_tags = set(owner.tags or [])
        candidate_tokens = set(candidate.tags or []) | set(candidate.interests or [])
        shared_tokens = [item for item in owner_tags & candidate_tokens if item]
        if shared_tokens:
            score += min(0.45, len(shared_tokens) * 0.12)
            evidence.append(f"共同兴趣: {', '.join(shared_tokens[:2])}")
        preferred_cities = {self._normalize_space(item) for item in preference.preferred_cities if self._normalize_space(item)}
        candidate_city = self._normalize_space(candidate.city or "")
        owner_city = self._normalize_space(owner.city or "")
        if candidate_city and candidate_city in preferred_cities:
            score += 0.2
            evidence.append(f"城市偏好命中: {candidate.city}")
        elif candidate_city and owner_city and candidate_city == owner_city:
            score += 0.16
            evidence.append(f"同城匹配: {candidate.city}")
        if candidate.likes_owner:
            score += 0.1
            evidence.append("关系信号: 对方已留意你")
        if candidate.liked_by_owner:
            score += 0.08
            evidence.append("关系信号: 你已表达过好感")
        if candidate.verified:
            score += 0.04
        return score, evidence[:3]

    def _tokenize(self, text: str) -> set[str]:
        cleaned = self._normalize_space(text).lower()
        if not cleaned:
            return set()
        parts = re.split(r"[\s,，。；;、|/]+", cleaned)
        return {part for part in parts if len(part) >= 2}

    def _normalize_space(self, text: str) -> str:
        return re.sub(r"\s+", " ", str(text or "")).strip()

    def _cosine_similarity(self, left: list[float], right: list[float]) -> float:
        if not left or not right or len(left) != len(right):
            return 0.0
        numerator = sum(a * b for a, b in zip(left, right))
        left_norm = math.sqrt(sum(a * a for a in left))
        right_norm = math.sqrt(sum(b * b for b in right))
        if left_norm <= 0 or right_norm <= 0:
            return 0.0
        return numerator / (left_norm * right_norm)
