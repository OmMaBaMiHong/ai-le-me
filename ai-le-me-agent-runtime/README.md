# AiLeMe Agent Runtime

`ai-le-me-agent-runtime` is a standalone Python service for advanced persona intelligence and companion-agent workflows.

It is designed to sit beside:

- `multi-platform-app`
- `ai-le-me-admin`
- `ai-le-me-modules`

instead of inside them, so the runtime can later become an independent product.

## Why this service exists

The Java side already handles:

- users, chat, moments, dating,积分,风控,审计
- provider routing and business execution
- WebSocket push and permissions

This Python service focuses on the parts that become painful in Java once the product grows:

- long-running agent workflows
- richer persona inference from chat, labels, relationship graph, posts, and signals
- companion strategy generation for AI girlfriend / boyfriend assistants
- long-term memory and controlled skills

## Design principles

- Java remains the business source of truth.
- Python reads shared config from MySQL, but does not own core business writes.
- Runtime-specific data lives in its own tables.
- Skills are controlled plugins, not arbitrary code execution.
- Agent outputs should be structured JSON first, natural language second.

## Current capabilities

- `GET /health`
- `GET /runtime/config/current`
- `GET /runtime/skills`
- `GET /runtime/actions/catalog`
- `GET /runtime/architecture`
- `GET /runtime/traces`
- `GET /runtime/traces/{trace_id}`
- `POST /persona/report/generate`
- `POST /companion/reply/suggest`
- `POST /companion/strategy/next-step`
- `POST /companion/actions/preflight`

The first version ships with deterministic heuristics so the service is runnable before real model wiring. The LLM client boundary is already separated and can later be replaced by:

- direct OpenAI / Qwen / DeepSeek / Volcengine calls
- your existing MySQL-driven provider routing
- LangGraph / Agents SDK orchestration

## Directory layout

```text
ai-le-me-agent-runtime/
├── agent_runtime/
│   ├── api/
│   ├── core/
│   ├── repositories/
│   ├── schemas/
│   ├── services/
│   └── skills/
├── sql/
│   └── 001_agent_runtime_tables.sql
├── .env.example
├── pyproject.toml
└── README.md
```

## Shared configuration strategy

This runtime reads from the existing tables:

- `sys_third_party_provider`
- `sys_third_party_route_rule`

Recommended ownership:

- Java and admin UI continue managing config.
- Python reads those tables in read-only mode.
- Python keeps its own runtime tables for memory, reports, plans, and audit trails.

## Skills model

The skill system is inspired by the extensibility direction of projects like OpenClaw, but with tighter guardrails:

- every skill must be registered in code
- each skill exposes metadata first
- dangerous capabilities should require explicit allow-listing
- voice, external tools, and automation are adapters, not implicit shell access

Built-in placeholder skills included now:

- `voice_bridge`
- `relationship_radar`
- `moment_ghostwriter`

## Recommended stack

The target landing stack for this repo is:

- Java business core as source of truth and execution layer
- Python runtime as the agent intelligence boundary
- LangGraph for multi-step workflow orchestration
- Mem0 and Letta for long-term memory layering
- Graphiti and Neo4j for relationship graph intelligence
- OpenClaw used as inspiration for controlled skills / voice / channel adapters

The full selection table and rollout plan live in:

- `/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/docs/agent-stack-landing.md`

The formal product architecture for standalone commercialization lives in:

- `/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime/docs/agent-product-architecture.md`

## Environment

Copy `.env.example` and fill in the DB connection:

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime
/opt/homebrew/bin/python3.12 -m venv .venv
source .venv/bin/activate
pip install -e .[dev]
cp .env.example .env
uvicorn agent_runtime.main:app --reload --port 8091
```

The runtime baseline is now Python 3.12 so newer agent libraries can be adopted without
keeping legacy 3.9 compatibility workarounds around.

## Local AI Stack

For local MVP testing, this runtime now expects:

- Ollama for local embeddings on `11434`
- ChromaDB for vector recall on `8008`
- Neo4j for relationship graph facts on `7474/7687`

You can start the local stack with:

```bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-agent-runtime
docker compose -f deploy/docker-compose.local-stack.yml up -d
ollama pull nomic-embed-text
uvicorn agent_runtime.main:app --reload --port 8091
```

The runtime keeps graceful fallback behavior if one of these services is temporarily unavailable,
but the recommended MVP path is to run all three locally.

## Integration roadmap

### Phase 1

- Java calls Python for persona v2 and companion suggestions.
- Java still executes message sending and moment creation.
- Python stores memory and action plans in its own tables.

### Phase 2

- add model adapter layer with MySQL-driven route resolution
- add retrieval and memory condensation jobs
- add evidence-aware persona scoring

### Phase 3

- add controlled skills such as voice synthesis / voice transcription / calendar-style reminder hooks
- add human-in-the-loop action approval
- add semi-automatic companion workflows

## Suggested Java integration contract

Java should send already-authorized, sanitized, and tenant-safe payloads such as:

- user summary
- target user summary
- recent messages
- labels
- relationship signals
- recent moments
- permissions / limits / risk flags

This avoids hard-coupling the Python runtime to every business table join.
