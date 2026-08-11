# Cost and Observability Specification

Capability: `cost-and-observability`
Description: Real cost from day 1 (tokens + tool cost), structured logs, and per-run metrics. No stub-zero.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning cost/observability.

> Source: enriched doc §8 RNF3 (L811-829); §8 RNF7 (L899-909). Decision #4 (real cost from day 1).

## ADDED Requirements

### Requirement: Real Cost from Day 1

Every `WorkerRun` MUST record a real `estimated_cost` composed of LLM token cost (from `LlmClient`) plus tool cost (from `ToolGateway`), persisted in the `WorkerRun` and in the corresponding `AuditEvent`s. A stub that returns zero cost is NOT acceptable; even stub implementations MUST return a non-zero representative value so the cost pipeline is exercised from day 1.

#### Scenario: Stub returns non-zero cost

- GIVEN a `FakeLlmClient` stub returns tokens=120 with a unit price
- WHEN the run completes
- THEN `WorkerRun.estimated_cost` is non-zero
- AND the `LLM_CALLED` audit event carries the same non-zero cost
> Source: doc §8 RNF7 (L899-908); decision #4.

#### Scenario: Cost answers savings question

- GIVEN a set of completed runs
- WHEN an operator queries total cost vs estimated savings
- THEN per-run cost, model used, and escalation count are available
- AND cost-per-run and cost-vs-savings can be computed
> Source: doc §8 RNF7 (L904-908).

### Requirement: Per-Run Observability Metrics

The system MUST expose, per `WorkerRun`: total duration, per-step duration, final state, estimated cost, worker version, policy version, LLM provider/model, number of recovered sources, number of escalations, and block/escalation reason. Logs MUST be structured.

#### Scenario: Metrics available per run

- GIVEN a completed run
- WHEN an operator inspects observability
- THEN total duration, per-step latency, cost, model, source count, and escalation count are present
> Source: doc §8 RNF3 (L815-828).

### Requirement: Cost Threshold Triggers Policy

When `estimated_cost` exceeds a configured threshold, policy P-012 MUST `ESCALATE` or `BLOCK`.

#### Scenario: Cost threshold exceeded

- GIVEN a run whose accumulated cost exceeds the threshold
- WHEN P-012 evaluates
- THEN the decision is `ESCALATE` or `BLOCK`
- AND the stop-criterion pipeline is informed
> Source: doc §7 RF9 P-012 (L640); §12 CL11 (L1117).
