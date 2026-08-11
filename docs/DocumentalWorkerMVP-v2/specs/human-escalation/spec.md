# Human Escalation Specification

Capability: `human-escalation`
Description: `HumanEscalation` creation, review lifecycle, SLA enforcement, default owner resolution, and the block on auto-closing an unreviewed escalation as success.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning human escalation.

> Source: enriched doc §7 RF10 (L644-692); §11 CU7 (L1082-1100); §12 CL12 (L1118). Decisions #8 (SLA), #11 (default owner).

## ADDED Requirements

### Requirement: HumanEscalation Entity

The system MUST create a `HumanEscalation` when the worker cannot safely close a run, with fields `id`, `tenant_id`, `worker_run_id`, `reason`, `risk_level`, `status`, `assigned_to`, `created_at`, `reviewed_at`, `resolved_at`, `sla_due_at`, `resolution_type`, `resolution_notes`. `status` MUST be one of `PENDING`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `RESOLVED`, `EXPIRED`. `reason` MUST be one of: `INSUFFICIENT_EVIDENCE`, `CONTRADICTORY_SOURCES`, `SENSITIVE_TOPIC`, `PROMPT_INJECTION`, `TOOL_NOT_ALLOWED`, `TENANT_ISOLATION_RISK`, `JSON_VALIDATION_FAILED`, `HIGH_RISK_ACTION`, `AMBIGUOUS_REQUEST`, `COST_LIMIT_EXCEEDED`, `INVALID_ANSWER`.

#### Scenario: Escalation linked to run

- GIVEN a run that cannot complete safely
- WHEN an escalation is created
- THEN it references the originating `worker_run_id`
- AND the run ends as `ESCALATED`
> Source: doc §7 RF10 (L646-684).

### Requirement: SLA by Risk Level and Breach Handling

SLA MUST be: `HIGH=4h laborables`, `MEDIUM=1 día laboral`, `LOW=2 días laborables` (decision #8). On SLA expiry, the escalation status MUST move to `EXPIRED`/`SLA_BREACHED` and the run MUST NOT be auto-closed as success.

#### Scenario: SLA breach blocks auto-success

- GIVEN an escalation whose `sla_due_at` has passed without resolution
- WHEN the breach is detected
- THEN the escalation is marked `SLA_BREACHED`
- AND the originating run cannot be closed as `COMPLETED`
- AND an operational problem flag is raised
> Source: doc §7 RF10 (L686-691); §12 CL12 (L1118); decision #8.

### Requirement: Default Owner Resolution

If `tenant.default_escalation_owner` is set, the escalation `assigned_to` MUST be that owner. If it is NOT set, the escalation `assigned_to` MUST be `UNASSIGNED` and the system MUST emit a `configuration_required` audit event. The system MUST NEVER invent a user.

#### Scenario: Tenant has default owner

- GIVEN a tenant with `default_escalation_owner` configured
- WHEN an escalation is created
- THEN `assigned_to` equals that owner
- AND no `configuration_required` event is emitted
> Source: doc §7 RF10 (L644-692); decision #11.

#### Scenario: Tenant lacks default owner

- GIVEN a tenant with no `default_escalation_owner`
- WHEN an escalation is created
- THEN `assigned_to` is `UNASSIGNED`
- AND a `configuration_required` audit event is emitted
> Source: doc §7 RF10; decision #11.

### Requirement: Review Lifecycle (CU7)

A reviewer MUST be able to move an escalation through `PENDING → IN_REVIEW → APPROVED|REJECTED|RESOLVED`, recording `resolution_type` and `resolution_notes`. Review time MUST be measured and the link to the original run MUST be preserved.

#### Scenario: Reviewer resolves escalation

- GIVEN an escalation in `IN_REVIEW`
- WHEN the reviewer sets `status=RESOLVED` with `resolutionType=APPROVED_WITH_NOTES`
- THEN the escalation and originating `WorkerRun` are updated
- AND `reviewed_at` and `resolved_at` are populated
> Source: doc §11 CU7 (L1082-1100); §13.5 (L1211-1225).

### Requirement: Unreviewed Escalation Cannot Close as Success

An escalation that is not reviewed (`PENDING` or `IN_REVIEW`) MUST NOT allow the originating run to be closed as `COMPLETED`.

#### Scenario: Pending escalation blocks success

- GIVEN an escalation still in `PENDING`
- WHEN an attempt is made to mark the run `COMPLETED`
- THEN the attempt is rejected
- AND the run remains `ESCALATED`
> Source: doc §7 RF10 (L690-691); §12 CL12 (L1118).
