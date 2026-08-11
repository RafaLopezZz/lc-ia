# Worker Run Lifecycle Specification

Capability: `worker-run-lifecycle`
Description: The `WorkerRun` execution lifecycle — states, transitions, duration/cost recording, and tenant resolution per request.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning run state and ordered execution.

> Source: enriched doc §7 RF2 (L307-346); §11 CU1 (L963-990); §21 vertical slice (L1605-1622). Decision #10 (tenant from secure context).

## ADDED Requirements

### Requirement: WorkerRun Entity and Final States

The system MUST persist each documentary task as a `WorkerRun` with fields `id`, `tenant_id`, `worker_id`, `requested_by`, `objective`, `status`, `risk_level`, `worker_version`, `policy_version`, `llm_provider`, `llm_model`, `estimated_cost`, `started_at`, `finished_at`, `requires_human_escalation`, `final_answer_summary`, `failure_reason`. `status` MUST be one of `RUNNING`, `COMPLETED`, `FAILED`, `ESCALATED`, `BLOCKED`. Every run MUST reach exactly one final state; orphan runs without a worker MUST NOT exist.

#### Scenario: Run records duration and cost

- GIVEN a run starts and ends
- WHEN the run reaches a final state
- THEN `started_at` and `finished_at` are populated
- AND `estimated_cost` is populated with a non-default value (see cost-and-observability)
> Source: doc §7 RF2 (L339-345).

### Requirement: Ordered Documentary Flow

A run MUST execute in order: policy check → `document.search` → structured answer → evidence validation → respond or escalate. The final state MUST be `COMPLETED` only when evidence is sufficient; otherwise `ESCALATED`, `BLOCKED`, or `FAILED`. No later step MUST be treated as successful once an earlier step has failed.

#### Scenario: Complete run with evidence (CU1)

- GIVEN an active worker, an active tenant, and authorized tenant documents
- WHEN the user starts a documentary run
- THEN the run records `POLICY_CHECK`, `TOOL_CALL`, `AI_CALL`, `EVIDENCE_VALIDATION` steps in order
- AND the final state is `COMPLETED`
- AND the response includes at least one cited source
> Source: doc §11 CU1 (L963-990); §21 (L1609-1622).

### Requirement: Tenant Resolution on Run Creation

On run creation, the `tenant_id` MUST be resolved from the authenticated secure context (decision #10). In `prod` profile, a client-supplied `tenantId` in the path, body, or header MUST be rejected; the path `tenantId` MUST match the resolved principal's tenant.

#### Scenario: Path tenant mismatches principal in prod

- GIVEN the `prod` profile and a principal whose tenant is A
- WHEN a request targets `/tenants/B/workers/{w}/runs`
- THEN the request is rejected
- AND an audit event records the mismatch attempt
> Source: doc §8 RNF1 (L773-789); §13.1 (L1135); decision #10.

### Requirement: Failed Step Stops the Run

If a step cannot continue safely, the run MUST end as `FAILED` or `ESCALATED` and no subsequent step MUST be treated as successful.

#### Scenario: Failed tool stops run

- GIVEN a `TOOL_CALL` step fails (e.g. ToolGateway timeout)
- WHEN the system cannot continue safely
- THEN the run final state is `FAILED` or `ESCALATED`
- AND the failure reason is recorded
> Source: doc §7 RF2 (L341-345); §12 CL17 (L1123).
