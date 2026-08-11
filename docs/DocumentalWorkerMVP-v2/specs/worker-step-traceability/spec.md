# Worker Step Traceability Specification

Capability: `worker-step-traceability`
Description: Ordered `WorkerStep` records within a `WorkerRun`, capturing each lifecycle phase with latency and error context.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning step records.

> Source: enriched doc §7 RF3 (L349-393).

## ADDED Requirements

### Requirement: WorkerStep Entity

The system MUST decompose each `WorkerRun` into ordered `WorkerStep` records with fields `id`, `tenant_id`, `worker_run_id`, `step_order`, `step_type`, `status`, `input_summary`, `output_summary`, `latency_ms`, `started_at`, `finished_at`, `error_code`, `error_message`. `step_type` MUST be one of `POLICY_CHECK`, `TOOL_CALL`, `AI_CALL`, `EVIDENCE_VALIDATION`, `ESCALATION`, `AUDIT`. `status` MUST be one of `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `SKIPPED`, `BLOCKED`.

#### Scenario: Steps preserve order

- GIVEN a run that executes policy, tool, LLM, and evidence steps
- WHEN the steps are persisted
- THEN `step_order` reflects execution sequence
- AND querying by `worker_run_id` ordered by `step_order` reconstructs the flow
> Source: doc §7 RF3 (L387-389).

### Requirement: Step Summaries Avoid Sensitive Data

An `AI_CALL` step MUST record an input/output summary but MUST NOT persist full prompts when they contain sensitive data. A `TOOL_CALL` step MUST record tool name, input summary, and output summary. A `POLICY_CHECK` step MUST record the policy decision and policy version.

#### Scenario: LLM step records summary not full prompt

- GIVEN an LLM call whose prompt contains tenant-sensitive content
- WHEN the `AI_CALL` step is persisted
- THEN `input_summary` and `output_summary` are stored
- AND the full prompt is NOT stored
> Source: doc §7 RF3 (L391); §8 RNF2 (L801).

### Requirement: Failed or Blocked Step is Recorded

A failed tool call, a blocked policy check, and a blocked tool (unauthorized) MUST each be recorded as a `WorkerStep` with the appropriate `error_code`/`error_message` and `status`.

#### Scenario: Unauthorized tool is recorded as blocked step

- GIVEN a worker whose `allowed_tools` does not include the requested tool
- WHEN the run attempts the tool call
- THEN a `TOOL_CALL` step is persisted with `status=BLOCKED`
- AND `error_code` identifies the unauthorized-tool policy
> Source: doc §7 RF3 (L390); §7 RF9 P-003 (L626).
