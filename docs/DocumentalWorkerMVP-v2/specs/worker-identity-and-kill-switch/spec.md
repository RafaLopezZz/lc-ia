# Worker Identity and Kill Switch Specification

Capability: `worker-identity-and-kill-switch`
Description: `WorkerIdentity` configuration, status lifecycle, allowed tools, and the kill switch that pauses a worker or all workers of a tenant without code deployment.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning worker identity and kill switch.

> Source: enriched doc §7 RF1 (L274-304); §7 RF12 (L731-746); §11 CU4 (L1033-1051).

## ADDED Requirements

### Requirement: WorkerIdentity Entity

The system MUST register each worker as `WorkerIdentity` with fields `id`, `tenant_id`, `name`, `worker_type`, `status`, `version`, `policy_version`, `allowed_tools`, `max_risk_level`, `created_at`, `updated_at`. `status` MUST be one of `ACTIVE`, `PAUSED`, `DISABLED`. `allowed_tools` MUST default to `["document.search"]` for the MVP.

#### Scenario: Active worker may run

- GIVEN a worker whose `status` is `ACTIVE` and whose tenant is `ACTIVE`
- WHEN a run is requested
- THEN the run proceeds past the identity policy check
- AND a `POLICY_CHECKED` step is recorded
> Source: doc §7 RF1 (L298-303).

### Requirement: Paused/Disabled Worker Blocks New Runs

If `WorkerIdentity.status` is `PAUSED` or `DISABLED`, any new `WorkerRun` for that worker MUST end as `BLOCKED` (policy P-001). The block MUST emit an `AuditEvent` of type `RUN_BLOCKED` and MUST record the applied policy. No LLM call or tool call MUST occur after the block.

#### Scenario: Kill switch on a single worker

- GIVEN an admin pauses a worker via `PATCH /workers/{workerId}/status` with `status=PAUSED`
- WHEN a user subsequently requests a run for that worker
- THEN the run is `BLOCKED`
- AND no LLM is invoked and no tool is executed
- AND the user receives a controlled message, not a raw technical error
> Source: doc §7 RF1 (L300-303); §7 RF12 (L740-745); §11 CU4 (L1046-1050).

### Requirement: Tenant-Wide Kill Switch

The system MUST support pausing all workers of a tenant by setting `Tenant.status=PAUSED` (or `DISABLED`) without a code deployment. All new runs for every worker of that tenant MUST be `BLOCKED` (policy P-002), and the operation MUST be audited as `KILL_SWITCH_APPLIED`.

#### Scenario: Tenant kill switch blocks every worker

- GIVEN a tenant is paused
- WHEN runs are requested for any of its workers
- THEN each run is `BLOCKED`
- AND a `KILL_SWITCH_APPLIED` audit event is emitted
> Source: doc §7 RF12 (L731-746); §7 RF9 P-002 (L625).

### Requirement: Kill Switch Returns Controlled Response

When a run is blocked by kill switch, the API MUST return a controlled, non-technical message to the caller and MUST NOT expose internal error stacks.

#### Scenario: Controlled message on blocked run

- GIVEN a worker is `PAUSED`
- WHEN the user requests a run
- THEN the response body states the worker is unavailable
- AND no stack trace or internal identifier beyond the run id is exposed
> Source: doc §7 RF12 (L745); §11 CU4 (L1050).
