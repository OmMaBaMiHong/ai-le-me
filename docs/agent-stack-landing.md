# Social Agent Stack Landing

This document summarizes the recommended landing stack for AiLeMe and maps the most relevant open-source projects to concrete product responsibilities.

## Recommended Stack

| Layer | Chosen Direction | Responsibility In AiLeMe | Landing Decision |
| --- | --- | --- | --- |
| Business core | Existing Java backend | User, chat, moments, dating permissions, points, risk control, audit, websocket push | Keep as source of truth |
| Agent runtime | Existing `ai-le-me-agent-runtime` | Persona reasoning, companion strategy, structured agent outputs, future long-running jobs | Keep as standalone Python service |
| Workflow orchestration | LangGraph | Multi-step workflows, resumable jobs, approval gates, hongniang pipelines, relationship-stage transitions | Adopt as the main workflow engine |
| Long-term memory | Mem0 + Letta | Mem0 for lightweight fact memory, Letta for deeper stateful companion continuity | Use dual-layer memory instead of one memory product forcing every use case |
| Relationship graph | Graphiti + Neo4j | Graphiti for temporal relationship updates, Neo4j for graph queries, explainable recommendation, and risk graph analysis | Introduce after phase 1 runtime is stable |
| Skills / voice / channels | OpenClaw-inspired controlled adapters | Voice bridge, channel connectors, skill metadata, external capability boundaries | Borrow design ideas only; do not use as trusted execution core |

## Open-Source Selection Table

| Project | Official Source | Best Use In AiLeMe | Why It Fits | Decision |
| --- | --- | --- | --- | --- |
| LangGraph | [GitHub](https://github.com/langchain-ai/langgraph) | Companion workflows, hongniang pipelines, approval steps, resumable tasks | Strong fit for long-running graph workflows and human-in-the-loop operations | Core orchestration choice |
| Letta | [GitHub](https://github.com/letta-ai/letta) | Stateful companion sessions, persistent persona continuity, owner-assistant memory | Best suited for stateful agents that should preserve long context and identity over time | Secondary deep-memory plane |
| Mem0 | [GitHub](https://github.com/mem0ai/mem0) | User preference memory, behavior fact extraction, chat recall | Good as a lightweight memory layer that can sit in front of multiple agent flows | Primary fact-memory layer |
| Graphiti | [GitHub](https://github.com/getzep/graphiti) | Relationship-stage graph, evolving people-topic-intent edges, time-aware social signals | Strong match for temporal relationship understanding instead of static user labels only | Primary relationship graph builder |
| Neo4j | [Official use case](https://neo4j.com/use-cases/real-time-recommendation-engine/) | Match recommendation, recommendation explanation, fraud links, introduction chains | Mature LPG query/storage option for recommendation and explainability | Production graph query/store target |
| OpenClaw | [Skills docs](https://docs.openclaw.ai/tools/skills) | Skills model, voice adapter thinking, channel expansion | Useful product inspiration for extensible tools and channels | Inspiration only |
| Dify | [GitHub](https://github.com/langgenius/dify) | Internal experimentation, prompt ops, workflow prototyping, ops console ideas | Good for experimentation but not ideal as the primary trusted runtime inside this stack | Optional internal ops tool |
| Companion App | [GitHub](https://github.com/a16z-infra/companion-app) | Companion UX and memory product inspiration | Helpful for product shape reference, not a production-ready social core | Inspiration only |

## Why This Hybrid Stack

### 1. Java should stay in charge of execution

Java already owns the hard business boundaries:

- who is allowed to talk to whom
- whether an invite is allowed
- points, permissions, subscriptions, and risk control
- audit trail and push channels

That means Python should not directly become the business owner. It should become the intelligence boundary.

### 2. One memory system is not enough

There are at least two very different memory problems in this product:

- lightweight memory of user facts, likes, signals, red flags, and chat-derived preferences
- deeper stateful companion continuity where the assistant keeps a stable understanding of the owner and the evolving relationship

Using Mem0 for the first and Letta for the second keeps the stack modular.

### 3. Recommendation and companion are related but not the same graph problem

Relationship-stage tracking and temporal social facts are not identical to heavy graph querying for recommendation and risk analysis.

- Graphiti is better aligned with changing relationship context
- Neo4j is better aligned with scalable LPG querying, explanation, and recommendation/risk patterns

## Product Capability Mapping

| Product capability | Main stack | Notes |
| --- | --- | --- |
| Persona report generation | Python runtime + Mem0 + Java facts | Start with structured evidence outputs, then add memory-backed retrieval |
| AI girlfriend / boyfriend assistant | Python runtime + LangGraph + Mem0 + Letta | LangGraph manages approval-gated workflows; Letta keeps deeper state |
| Intelligent hongniang | Java + LangGraph + Graphiti + Neo4j | Best solved as workflow + graph + recommendation explanation |
| Match recommendation | Java + Neo4j + Graphiti + persona features | Use Java for business filtering, graph for ranking/explanation |
| Auto content draft / dynamic / video prompts | Java + Python runtime + controlled skills | Keep publish and task creation owned by Java |
| Voice companion | Controlled skill adapter inspired by OpenClaw | Add later, not in phase 1 |

## Suggested Rollout

### Phase 1: Stabilize the runtime boundary

- Keep Java as business source of truth.
- Use Python runtime for persona report, reply suggestions, and next-step strategy.
- Store structured outputs and audit traces.
- Do not auto-execute high-risk social actions.

### Phase 2: Add workflow and memory

- Introduce LangGraph into Python runtime for multi-step orchestration.
- Add Mem0 as the default lightweight memory write/read layer.
- Add Letta only for selected deep companion sessions and owner-specific continuity.
- Keep Java approval checks ahead of any relationship-escalation action.

### Phase 3: Add relationship graph

- Build Graphiti-backed temporal relationship facts from chats, tags, interactions, and moments.
- Add Neo4j as the graph query/recommendation layer.
- Use graph results to explain matchmaking and next-step decisions.

### Phase 4: Add controlled skills and channels

- Keep skills code-registered and approval-gated.
- Use OpenClaw as inspiration for skills metadata, voice adapters, and channel abstractions.
- Do not hand business execution authority to third-party skill runtimes.

## What Should Be Implemented In This Repo

### Java

- business permissions
- execution of message send / moment publish / invite creation
- approval, risk control, billing, audit
- websocket and app-facing APIs

### Python runtime

- orchestration entrypoints
- persona reasoning and structured outputs
- companion strategy and memory boundary
- graph and memory adapter layer
- future voice / channel adapter registration

## Practical Repo Direction

The practical implementation direction for this repository is:

1. Keep `ai-le-me-modules` as the trusted business core.
2. Keep `ai-le-me-agent-runtime` as an independent Python service in the monorepo.
3. Add a workflow adapter layer for LangGraph in the runtime.
4. Add memory adapter layers for Mem0 and Letta in the runtime.
5. Add graph adapter layers for Graphiti and Neo4j in the runtime.
6. Keep skills controlled, code-registered, and Java-approved.

## Reference Links

- LangGraph: [https://github.com/langchain-ai/langgraph](https://github.com/langchain-ai/langgraph)
- Letta: [https://github.com/letta-ai/letta](https://github.com/letta-ai/letta)
- Mem0: [https://github.com/mem0ai/mem0](https://github.com/mem0ai/mem0)
- Graphiti: [https://github.com/getzep/graphiti](https://github.com/getzep/graphiti)
- Neo4j recommendation use case: [https://neo4j.com/use-cases/real-time-recommendation-engine/](https://neo4j.com/use-cases/real-time-recommendation-engine/)
- OpenClaw skills docs: [https://docs.openclaw.ai/tools/skills](https://docs.openclaw.ai/tools/skills)
- Dify: [https://github.com/langgenius/dify](https://github.com/langgenius/dify)
- Companion App: [https://github.com/a16z-infra/companion-app](https://github.com/a16z-infra/companion-app)
