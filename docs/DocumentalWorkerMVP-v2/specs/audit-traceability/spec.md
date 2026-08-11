# Audit Traceability Specification

Capability: `audit-traceability`
Description: The `AuditEvent` stream and the 12-event catalog that reconstruct any run from request to final state.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning audit.

> Source: enriched doc §7 RF11 (L695-728); §1.3 (L64-79). Decision #4 (real cost persisted in AuditEvent).

## ADDED Requirements

### Requirement: AuditEvent Entity

Every relevant action MUST produce an `AuditEvent` with fields `id`, `tenant_id`, `worker_id`, `worker_run_id`, `event_type`, `action`, `result`, `policy_version`, `policy_decision`, `tool_name`, `created_at`, `metadata`. `metadata` MUST be able to carry tokens, cost, and duration summaries (decision #4).

#### Scenario: Event is tenant-scoped

- GIVEN an event is produced for a run
- WHEN it is persisted
- THEN `tenant_id` matches the run's tenant
- AND the event is queryable by `worker_run_id`
> Source: doc §7 RF11 (L697-712).

### Requirement: Twelve-Event Catalog

The system MUST support at minimum these event types: `WORKER_RUN_CREATED`, `POLICY_CHECKED`, `TOOL_CALLED`, `LLM_CALLED`, `EVIDENCE_VALIDATED`, `RUN_COMPLETED`, `RUN_ESCALATED`, `RUN_BLOCKED`, `RUN_FAILED`, `HUMAN_ESCALATION_CREATED`, `HUMAN_ESCALATION_RESOLVED`, `KILL_SWITCH_APPLIED`.

#### Scenario: Complete run emits its events

- GIVEN a run that completes with cited evidence
- WHEN an auditor reviews the run
- THEN the event stream includes `WORKER_RUN_CREATED`, `POLICY_CHECKED`, `TOOL_CALLED`, `LLM_CALLED`, `EVIDENCE_VALIDATED`, `RUN_COMPLETED`
- AND the auditor can reconstruct request, steps, policies, sources, and final state
> Source: doc §7 RF11 (L714-727); §1.3 (L66-77).

#### Scenario: Blocked run emits its events

- GIVEN a run blocked by kill switch
- WHEN an auditor reviews the run
- THEN the stream includes `WORKER_RUN_CREATED`, `POLICY_CHECKED`, `KILL_SWITCH_APPLIED`, `RUN_BLOCKED`
- AND reason, policy gate, actor context, and timestamp are present
> Source: doc §7 RF11 (L723-727); §11 CU4 (L1041-1044).

### Requirement: Cost Recorded in AuditEvent

`LLM_CALLED` and `TOOL_CALLED` events MUST persist token counts and tool cost in `metadata` as real non-zero values when the provider/gateway returns them (decision #4). Stub-zero is NOT acceptable.

#### Scenario: LLM_CALLED carries non-zero cost

- GIVEN an LLM call returned tokens=120 and cost=0.012
- WHEN the `LLM_CALLED` event is persisted
- THEN `metadata` contains `tokens=120` and `cost=0.012`
> Source: doc §7 RF11 (L697-712); §8 RNF7 (L899-908); decision #4.

### Requirement: Audit is Not a Sensitive-Data Store

The audit trail MUST store what is necessary to reconstruct a run but MUST NOT become a permanent store of sensitive prompt content (RNF2). Summaries MUST be used where full content is sensitive.

#### Scenario: Sensitive prompt summarized

- GIVEN an `LLM_CALLED` event whose prompt is sensitive
- WHEN the event is persisted
- THEN `metadata` stores a summary, not the full prompt
> Source: doc §8 RNF2 (L801-807); §7 RF3 (L391).
