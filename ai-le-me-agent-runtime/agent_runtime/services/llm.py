from __future__ import annotations

import json
import logging
from typing import Any

import httpx

from agent_runtime.repositories.runtime_config import ResolvedRoute
from agent_runtime.services.router import SharedConfigRouter

logger = logging.getLogger(__name__)


class StructuredLlmService:
    def __init__(self, router: SharedConfigRouter, timeout_seconds: int = 45) -> None:
        self.router = router
        self.timeout_seconds = timeout_seconds

    def resolve_route(self, scene_code: str, function_type: str) -> ResolvedRoute:
        return self.router.resolve_ai_route(scene_code=scene_code, function_type=function_type)

    def complete_json(
        self,
        route: ResolvedRoute,
        system_prompt: str,
        user_payload: dict[str, Any],
        fallback: dict[str, Any] | None = None,
    ) -> dict[str, Any] | None:
        configs = route.configs or {}
        api_key = str(configs.get("api_key") or "").strip()
        endpoint = str(configs.get("endpoint") or "").strip().rstrip("/")
        model = str(configs.get("model") or configs.get("model_name") or "").strip()
        api_protocol = str(configs.get("api_protocol") or "").strip().lower() or "chat_completions"
        route_timeout = int(str(configs.get("timeout") or 0).strip() or 0)
        reasoning_effort = str(configs.get("reasoning_effort") or "").strip()
        max_output_tokens = int(str(configs.get("max_output_tokens") or 0).strip() or 0)
        timeout = max(route_timeout, self.timeout_seconds)

        if not endpoint or not model:
            return fallback
        if not api_key:
            return fallback

        user_content = json.dumps(user_payload, ensure_ascii=False)
        try:
            with httpx.Client(timeout=httpx.Timeout(float(timeout), connect=8.0)) as client:
                if api_protocol == "responses":
                    raw = self._call_responses(
                        client,
                        endpoint,
                        api_key,
                        model,
                        system_prompt,
                        user_content,
                        reasoning_effort,
                        max_output_tokens,
                    )
                else:
                    raw = self._call_chat_completions(
                        client,
                        endpoint,
                        api_key,
                        model,
                        system_prompt,
                        user_content,
                        max_output_tokens,
                    )
            parsed = self._parse_json(raw)
            return parsed or fallback
        except Exception as exc:
            logger.warning("Structured LLM request failed for provider=%s model=%s protocol=%s: %s", route.provider_code, model, api_protocol, exc)
            return fallback

    def _call_chat_completions(
        self,
        client: httpx.Client,
        endpoint: str,
        api_key: str,
        model: str,
        system_prompt: str,
        user_content: str,
        max_output_tokens: int,
    ) -> str:
        payload: dict[str, Any] = {
            "model": model,
            "temperature": 0.2,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content},
            ],
            "response_format": {"type": "json_object"},
        }
        if max_output_tokens > 0:
            payload["max_tokens"] = max_output_tokens
        response = client.post(
            f"{endpoint}/chat/completions",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json=payload,
        )
        response.raise_for_status()
        data = response.json()
        choices = data.get("choices") or []
        if not choices:
            return ""
        message = choices[0].get("message") or {}
        content = message.get("content") or ""
        if isinstance(content, list):
            return "\n".join(
                str(item.get("text") or item.get("content") or "")
                for item in content
                if isinstance(item, dict)
            )
        return str(content)

    def _call_responses(
        self,
        client: httpx.Client,
        endpoint: str,
        api_key: str,
        model: str,
        system_prompt: str,
        user_content: str,
        reasoning_effort: str,
        max_output_tokens: int,
    ) -> str:
        payload: dict[str, Any] = {
            "model": model,
            "input": [
                {"role": "system", "content": [{"type": "input_text", "text": system_prompt}]},
                {"role": "user", "content": [{"type": "input_text", "text": user_content}]},
            ],
        }
        if reasoning_effort:
            payload["reasoning"] = {"effort": reasoning_effort}
        if max_output_tokens > 0:
            payload["max_output_tokens"] = max_output_tokens
        response = client.post(
            f"{endpoint}/responses",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json=payload,
        )
        response.raise_for_status()
        data = response.json()
        output_text = data.get("output_text")
        if isinstance(output_text, str) and output_text.strip():
            return output_text
        output = data.get("output") or []
        parts: list[str] = []
        for item in output:
            if not isinstance(item, dict):
                continue
            for content in item.get("content") or []:
                if isinstance(content, dict):
                    text = content.get("text") or content.get("output_text") or ""
                    if text:
                        parts.append(str(text))
        return "\n".join(parts)

    def _parse_json(self, raw: str) -> dict[str, Any] | None:
        text = str(raw or "").strip()
        if not text:
            return None
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            start = text.find("{")
            end = text.rfind("}")
            if start >= 0 and end > start:
                try:
                    return json.loads(text[start : end + 1])
                except json.JSONDecodeError:
                    return None
        return None
