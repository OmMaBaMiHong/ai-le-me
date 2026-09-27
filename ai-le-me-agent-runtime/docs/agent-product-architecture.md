# AiLeMe Agent Runtime Product Architecture

## 1. Product Definition

`ai-le-me-agent-runtime` is not positioned as a one-off internal inference helper for AiLeMe.
It is the future standalone `relationship-agent runtime` for intimacy, dating, matchmaking,
and companion workflows.

Its product target is:

- to provide a Claude-Code-style agent kernel in a vertical relationship domain
- to expose typed tools, long-running tasks, memory, policy, and evaluation as platform capabilities
- to remain usable inside AiLeMe today while staying self-consistent as a future SaaS runtime

This means the runtime must serve two roles at the same time:

- `embedded mode`: AiLeMe calls it as the intelligence boundary while Java remains the business source of truth
- `standalone mode`: the runtime becomes a multi-tenant product with channel adapters, billing, policy, and operational tooling

## 2. North Star

Build an agent that can observe relationship signals, recall long-term context, reason over goals,
plan multi-step progression, execute typed actions under governance, and continuously improve from outcomes.

The target user experience is not "one-shot AI reply generation".
The target user experience is "a trustworthy relationship operator that can help, recommend, and automate
within user-defined boundaries".

## 3. Product Boundary

The runtime owns:

- agent reasoning and planning
- memory and retrieval orchestration
- typed tool selection
- workflow state and task loops
- policy pre-checks and execution recommendations
- trace, replay, evaluation, and optimization data

The host app owns:

- canonical business data and final writes
- payments, account balances, and bills
- compliance-sensitive execution controls
- channel delivery, user notifications, and audit obligations
- user-visible authorization and product packaging

This separation keeps the runtime reusable across products while protecting host-specific business rules.

## 4. Runtime Spine

The runtime spine is the stable execution backbone and should remain product-agnostic:

### 4.1 Observe

Collect and normalize:

- recent chat turns
- profile and persona snapshots
- moments, photos, and content cues
- recommendation signals from matchmaker
- event signals such as reply delay, conversation rounds, gift history, and date progress

### 4.2 Recall

Unify:

- short-term conversational context
- long-term memory summaries
- vector recall
- lexical recall
- graph relationship facts
- prior plans, executions, and outcomes

### 4.3 Reason

Produce structured judgments, including:

- relationship stage
- target preference hypotheses
- interaction risks
- progression readiness for WeChat exchange, date requests, gifts, and follow-up
- master-style adaptation based on persona and relationship evidence

### 4.4 Plan

Turn reasoning into executable plans:

- next turn suggestions
- next 24h / 72h strategy
- standing orders
- escalation conditions
- multi-step tasks such as "build familiarity -> qualify interest -> request date"

### 4.5 Policy

No execution should bypass policy.
This layer decides whether the runtime may recommend, auto-execute, defer, or require host-side confirmation.

Core checks include:

- VIP entitlement
- user capability switch
- object-level authorization
- quiet hours and pacing controls
- love-coin / token budget constraints
- risk grade and action allow-list
- compliance hooks for gifts, contact exchange, and offline invites

### 4.6 Act

All actions must be expressed as typed tools or structured action payloads, never free-form instructions.

Examples:

- `message_auto_send`
- `reply_draft_generate`
- `gift_plan_create`
- `date_slot_suggest`
- `moment_draft_create`
- `candidate_outreach_start`

### 4.7 Reflect

Write back:

- execution outcomes
- target responses
- failed or blocked actions
- updated relationship stage
- memory condensation results

### 4.8 Eval

Continuously measure:

- response quality
- safety violations prevented
- date conversion rate
- WeChat exchange conversion rate
- cost per successful progression
- retrieval usefulness
- tool success and rollback rate

`Eval` is intentionally explicit. Without it, the runtime cannot become a real product.

## 5. Product Planes

Beyond the runtime spine, a standalone product needs three persistent planes.

### 5.1 Identity / Tenant / Channel Plane

Responsibilities:

- tenant isolation
- workspace and environment separation
- channel adapter registration
- agent identity management
- object-scoped conversation isolation
- host-app and external API integration contracts

If this plane is missing, the runtime stays trapped as a single-app internal service.

### 5.2 Governance / Safety / Economics Plane

Responsibilities:

- capability switches and packaging
- policy rules and execution hooks
- token routing and provider budgets
- love-coin billing and host settlement
- action quotas and fail-safe degradation
- approval-mode compatibility even if the default product path is switch-based authorization

This plane is what makes the product sellable instead of merely clever.

### 5.3 Operations / Observability / Improvement Plane

Responsibilities:

- trace storage
- workflow replay
- prompt and route experiments
- regression datasets
- offline evaluation
- live KPI dashboards
- failure triage and rollback support

This plane is what allows the runtime to improve as an operating product.

## 6. Platform Modules

To make the product self-consistent, the runtime should be decomposed into the following modules:

### 6.1 Agent Kernel

Provides:

- stateful task loop
- typed tool invocation
- hook points
- multi-step progression execution
- recoverable workflow runs

Claude Code is most relevant here as inspiration, not as a direct copy target.

### 6.2 Memory Fabric

Provides:

- short-term context
- long-term summaries
- vector memory
- lexical memory
- graph memory
- memory condensation jobs

### 6.3 Persona Engine

Provides:

- owner persona
- target persona
- pair persona
- master-style mapping
- progression constraints and attraction hypotheses

### 6.4 Relationship Strategy Engine

Provides:

- stage evaluation
- next-step generation
- outreach strategy
- conversation pacing
- date / WeChat readiness assessment

### 6.5 Tool Registry

Provides:

- typed tool definitions
- input and output schema
- risk tags
- execution ownership
- policy binding
- host execution contract

### 6.6 Policy Engine

Provides:

- entitlement checks
- frequency and quiet-hour limits
- cost guardrails
- target authorization checks
- action escalation rules

### 6.7 Channel Gateway

Provides:

- message channels
- moments/content channels
- notification and push callbacks
- future external channel adapters

### 6.8 Trace and Eval Center

Provides:

- traces
- prompt artifacts
- retrieval evidence
- execution outcomes
- scorecards
- benchmark datasets

## 7. Core Product Surfaces

The runtime should serve three top-level agent products:

### 7.1 Persona Agent

Long-horizon structured understanding of the user and their relationship style.

### 7.2 Matchmaker Agent

Candidate recall, rerank, recommendation explanation, and proactive outreach suggestions.

### 7.3 Companion Agent

Object-centric relationship operation, including:

- proactive messaging
- reply assistance
- strategy progression
- date timing suggestions
- gift recommendations
- moment drafting
- follow-up loops

These are not separate silos.
They are three product surfaces built on one shared platform.

## 8. Business Model and Commercialization Constraints

For standalone commercialization, the runtime must support:

- multi-tenant isolation
- provider routing abstraction
- usage metering
- SKU-based capability packaging
- host-side revenue sharing or settlement
- explainable safety controls
- auditable execution history

Recommended monetization anchors:

- per-seat or per-active-agent fee
- token / external API pass-through
- premium workflow packs
- higher-tier memory depth and evaluation tooling
- enterprise policy and audit features

## 9. Build Principles

### 9.1 Productize the boundary, not just the model

The moat is not only the prompt.
The moat is the governed runtime boundary around the model.

### 9.2 Typed actions over free-form autonomy

High autonomy is allowed only through typed tools, policy hooks, and observable traces.

### 9.3 Java remains the source of truth until product extraction is complete

In AiLeMe mode, Java stays responsible for execution authority and billing truth.
The runtime should be extraction-ready without prematurely duplicating business ownership.

### 9.4 Relationship intelligence is evidence-driven

Every major decision should be explainable through:

- conversation evidence
- retrieval evidence
- graph evidence
- policy evidence

### 9.5 Eval is a first-class capability

No hidden agent logic that cannot be replayed, measured, or improved.

## 10. Current Gap Assessment

The current runtime already has:

- structured persona and companion generation
- memory and graph retrieval foundations
- route resolution
- skill registry
- trace persistence

The biggest gaps toward the standalone product are:

- no true agent kernel yet
- no first-class policy engine yet
- no standalone tenant/channel plane yet
- no full evaluation center yet
- no generalized typed tool contract across all actions yet

## 11. Delivery Direction

Near-term implementation should follow this order:

1. Formalize product architecture, runtime spine, policy surfaces, and eval metrics in code.
2. Introduce typed tool registry and runtime policy preflight decisions.
3. Introduce durable workflow runs and agent-kernel task loops.
4. Expand traces into eval-ready artifacts.
5. Add multi-tenant and channel abstractions before external commercialization.

## 12. Final Decision

The six-layer model is necessary but not sufficient.

The self-consistent standalone shape is:

- `runtime spine`: Observe / Recall / Reason / Plan / Policy / Act / Reflect / Eval
- `product planes`: Identity-Tenant-Channel / Governance-Economics / Operations-Improvement
- `platform modules`: kernel, memory, persona, strategy, tools, policy, gateway, eval

That is the product architecture this repository should evolve toward.
