# Tasks: DocumentalWorkerMVP-v2

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1700-2200 |
| 400-line budget risk | High |
| Chained PRs recommended | No (size:exception approved) |
| Suggested split | single PR (size:exception) — sequenced commits by cluster |
| Delivery strategy | ask-always |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: High

> USER APPROVED SIZE EXCEPTION: single PR with `size:exception` (~950 lines
> estimate at proposal; design-grown task breakdown grows to ~1700-2200
> during apply). Do NOT split. Apply sequentially via commit-per-work-unit
> (see `work-unit-commits`); order enables incremental verification.

### Suggested Work Units (commits inside the single PR; tests ship with code)

| Unit | Goal | Commit | Depends on |
|------|------|--------|------------|
| T0..T3 + T0b | Foundation: audit + tenant schema + context + error handling | commit 1 | — |
| T4..T5 | Worker identity + kill switch (RF1, RF7) | commit 2 | T1 |
| T6..T7 | Run orchestrator + step traceability + controller | commit 3 | T4 |
| T8 | Document corpus + ILIKE/FTS search (Phase 1) | commit 4 | T6 |
| T9..T10 | ToolGateway + document.search tool + cost | commit 5 | T8 |
| T11 | LlmClient contract + real cost (NOT stub-zero) | commit 6 | T9 |
| T12..T13 | StructuredAnswer contract + parser (invalid → ESCALATE) | commit 7 | T11 |
| T14 | EvidenceValidator (produces signals) | commit 8 | T13 |
| T23 | PilotValidationConfig (RVB1-3) — BEFORE policy/cost | commit 9 | T1 |
| T15..T17 | PolicyEngine (translates signals → decisions) + HybridDetector + audit | commit 10 | T14, T23 |
| T18..T19 | HumanEscalation + SLA + BusinessCalendar stub | commit 11 | T17 |
| T20 | AuditTraceability: complete 12-event catalog (completes T0) | commit 12 | T17 |
| T21 | CostAggregator + P-012 threshold (PilotValidationConfig.cost_threshold ready) | commit 13 | T11, T15, T23 |
| T22 | Data minimization + retention (RNF2) | commit 14 | T20 |
| T24..T25 | RF14 + RF15 progressive-optional (gate by T23) | commit 15 | T23, T14 |
| T26 | Integration tests incl. two-tenant isolation | commit 16 | T20, T18 |
| T27 | Supersede chore for prior `DocumentalWorkers` | commit 17 | gated by verify |

Cluster count: 17. Task count: 29 (T0..T7, T0b, T8..T27, with T23 moved up to Phase 9 and T-err renamed to T0b).

---

## Phase 1: Foundation (cluster 1 — includes new audit foundation + error handling)

- [ ] **T0 — Audit foundation: AuditRecorder port + AuditEvent entity + JpaAuditRecorder**
  - Cluster: Foundation
  - Spec refs: `spec: audit-traceability/spec.md`
  - Design refs: `design §8 (AuditRecorder port, AuditEvent entity)`
  - Doc refs: `doc §7 RF11 L695-729`
  - Acceptance:
    - `AuditRecorder` port interface defined in domain core.
    - `AuditEvent` entity with fields: `id`, `tenant_id`, `worker_run_id`, `event_type`, `severity`, `metadata` (JSONB), `recorded_at`.
    - `JpaAuditRecorder` minimal implementation: persist + query by `(tenant_id, worker_run_id)`.
    - **Supports at least these 7 initial events:**
      - `WORKER_RUN_CREATED`
      - `POLICY_CHECKED`
      - `TOOL_CALLED`
      - `LLM_CALLED`
      - `HUMAN_ESCALATION_CREATED`
      - `KILL_SWITCH_APPLIED`
      - `CLIENT_SUPPLIED_TENANT_REJECTED`
    - DDL for `audit_event` table (tenant_id NOT NULL + index) included in T1's migration script.
    - **Pre-auth / security events (no resolved tenant):** the migration inserts a bootstrap sentinel row `tenants(id='__system__', status='ACTIVE')`. Audit events that occur BEFORE tenant resolution (e.g. `CLIENT_SUPPLIED_TENANT_REJECTED` when the request is unauthenticated) persist with `tenant_id='__system__'`. This keeps `tenant_id NOT NULL` satisfied without special-casing the schema. If a tenant IS resolvable from auth context at rejection time, the real tenant_id is used instead.
    - T20 later completes the full 12-event catalog; T0 is the minimal substrate all early tasks need.
  - Deps: —
  - Size est: ~150 lines (port + entity + JPA recorder + 7-event enum + system tenant + tests)
  - **Why early:** every task that emits audit events (T3, T5, T7, T9, T10, T11, T15, T17, T18) depends on this being ready first.

- [ ] **T1 — Tenant schema + RLS-prep migration**
  - Cluster: Foundation
  - Spec refs: `spec: tenant-management/spec.md`
  - Design refs: `design §4/§9/§12`
  - Doc refs: `doc §7 RF0 L248-271`; `doc §8 RNF1 L773-789`
  - Acceptance: `V2__DocumentalWorkerMVP_v2.sql` creates every tenant-scoped table with `tenant_id VARCHAR NOT NULL` + `CREATE INDEX idx_<table>_tenant_id ON <table> (tenant_id)` per design §12 (PostgreSQL-compatible syntax); includes `audit_event` table defined by T0; **inserts bootstrap sentinel tenant** `INSERT INTO tenants(id, status) VALUES ('__system__', 'ACTIVE')` for pre-auth audit events; `__system__` tenant is excluded from business queries but allowed in `audit_event`; test inserting NULL `tenant_id` row throws constraint violation; Flyway applies cleanly onto prior `V1`.
  - Deps: T0 (audit_event DDL design)
  - Size est: ~260 lines (migration + null-tenant test)

- [ ] **T2 — Tenant module + entity repository**
  - Cluster: Foundation
  - Spec refs: `spec: tenant-management/spec.md`
  - Design refs: `design §3 (tenant)`
  - Doc refs: `doc §7 RF0 L248-271`
  - Acceptance: `com.leovinci.aiworkers.tenant.{Tenant,TenantRepository,TenantAdminController}` exist; repository exposes tenant-scoped helpers only; status enum ACTIVE/PAUSED/DISABLED; `default_escalation_owner` nullable.
  - Deps: T1
  - Size est: ~120 lines

- [ ] **T3 — TenantSecurityContextResolver** `[decision-pending: #3]`
  - Cluster: Foundation
  - Spec refs: `spec: tenant-management/spec.md`
  - Design refs: `design §9`
  - Doc refs: `doc §8 RNF1 L773-789`; `doc §13.1 L1132-1166`
  - Acceptance:
    - **Production:** `tenant_id` is resolved **only** from the authenticated secure context (e.g. JWT principal, mTLS certificate, or configured IdP token). No `X-Tenant-Id` header, request body field, or URL path parameter is accepted as the authoritative source of tenant identity in the `prod` profile.
      - If a client supplies a tenant_id via header/body/path attempting to override the context → **reject** the request (HTTP 400) **and** emit `CLIENT_SUPPLIED_TENANT_REJECTED` audit event with the rejected value recorded in `metadata`. If the auth context already resolved a tenant, that `tenant_id` is used on the audit row; otherwise the audit row falls back to the `__system__` sentinel tenant (T0) so the NOT NULL constraint holds.
      - If tenant cannot be resolved from auth context → respond **401** (unauthenticated) or **403** (authenticated but no tenant).
    - **The final JWT claim name** (e.g. `tenant_id`, `tid`, `org_id`) remains `[decision-pending: #3]` until coordinated with the IdP. The MVP resolver reads from a configurable `app.auth.tenant-claim` property defaulting to `tenant_id`.
    - **Development/Test:** `X-Tenant-Id` header is accepted **only** when the active Spring profile is `dev` or `test`. It must correspond to an existing ACTIVE tenant. It is **forbidden** in `prod`.
  - **MVP default (ship with):** configurable `app.auth.tenant-claim` = `tenant_id`; dev profile allows `X-Tenant-Id` validated against `TenantRepository`; prod rejects `X-Tenant-Id` unconditionally + audits the attempt.
  - Deps: T1, T2
  - Size est: ~150 lines

- [ ] **T0b — GlobalExceptionHandler (ProblemDetail / RFC 7807)**
  - Cluster: Foundation
  - Spec refs: cross-cutting (no single spec)
  - Design refs: `design §1` (modular monolith layering)
  - Doc refs: `doc §8 RNF1 L773-789`
  - Acceptance:
    - `@RestControllerAdvice` producing `ProblemDetail` (RFC 9457 / RFC 7807) JSON responses.
    - Never exposes stack traces in response bodies.
    - Includes `runId` in `metadata` when a `WorkerRun` context exists.
    - Controlled error types:
      - `TENANT_REJECTED` — client-supplied tenant override attempt.
      - `BLOCKED_RUN` — worker blocked by kill switch or policy.
      - `PROVIDER_UNAVAILABLE` — LLM / Tool gateway timeout or connection error.
      - `VALIDATION_ERROR` — input validation failures.
      - `FORBIDDEN_TENANT_ACCESS` — cross-tenant attempt.
      - `INVALID_REQUEST` — malformed payload.
    - Placed **before** any controller task (T5, T7, T19) to be available globally.
  - Deps: —
  - Size est: ~80 lines

## Phase 2: Worker identity + kill switch (cluster 2)

- [ ] **T4 — WorkerIdentity entity + repository**
  - Cluster: Worker identity
  - Spec refs: `spec: worker-identity-and-kill-switch/spec.md`
  - Design refs: `design §4 (WorkerIdentity)`
  - Doc refs: `doc §7 RF1 L274-306`; `doc §7 RF12 L731-748`
  - Acceptance: `WorkerIdentity` fields per design §4; `allowed_tools` JSONB default `["document.search"]`; `max_risk_level`; repository queries filtered by `tenant_id`.
  - Deps: T1
  - Size est: ~110 lines

- [ ] **T5 — WorkerKillSwitchService + controller**
  - Cluster: Worker identity
  - Spec refs: `spec: worker-identity-and-kill-switch/spec.md`
  - Design refs: `design §6.3`; `design §13 step 2`
  - Doc refs: `doc §7 RF12 L731-748`
  - Acceptance: `PATCH /api/v1/tenants/{tid}/workers/{wid}/status` PAUSED/DISABLED emits `KILL_SWITCH_APPLIED` (scope worker); tenant pause emits same with scope tenant; controlled message on blocked run (no stack, only runId); uses T0's `AuditRecorder`.
  - Deps: T4, T0 (AuditRecorder)
  - Size est: ~120 lines

## Phase 3: Run orchestrator + step traceability (cluster 3)

- [ ] **T6 — WorkerRun + WorkerStep entities + repositories**
  - Cluster: Run lifecycle
  - Spec refs: `spec: worker-run-lifecycle/spec.md`; `spec: worker-step-traceability/spec.md`
  - Design refs: `design §4 (WorkerRun, WorkerStep)`; `design §12`
  - Doc refs: `doc §7 RF2 L307-347`; `doc §7 RF3 L349-395`
  - Acceptance:
    - Enums: `RunState{RUNNING, COMPLETED, FAILED, ESCALATED, BLOCKED}`, `StepType{POLICY_CHECK, TOOL_CALL, AI_CALL, EVIDENCE_VALIDATION, ESCALATION, AUDIT}`.
    - **FAILED, BLOCKED, and ESCALATED are distinct terminal outcomes:**
      - **`FAILED`** = technical / infrastructure / non-recoverable exception (provider unavailable, timeout, network error, database constraint violation). No business processing may continue.
      - **`BLOCKED`** = hard policy or kill-switch stop — the worker is **forbidden** from proceeding by a deterministic rule (P-003 unauthorized tool, P-005 cross-tenant, P-006 clear bypass, kill switch active). No further business steps run; only `AUDIT` may follow.
      - **`ESCALATED`** = controlled functional outcome that requires human review (P-004 insufficient evidence, P-006 inconclusive invalid answer, P-007/P-008/P-009 inconclusive, P-010 unsupported claim). An `ESCALATION` step plus `AUDIT` steps **may** follow before the run closes.
    - **Step ordering rules:**
      - No business-processing step (`TOOL_CALL`, `AI_CALL`, `EVIDENCE_VALIDATION`) may be `COMPLETED` after a terminal `FAILED` or `BLOCKED`.
      - After `ESCALATED`, only `ESCALATION` and `AUDIT` steps may run (no further tool calls or LLM calls that progress the original objective).
    - Ordered steps persisted; tests assert correct sequencing for FAILED, ESCALATED, and BLOCKED terminal paths.
  - Deps: T4
  - Size est: ~190 lines

- [ ] **T7 — WorkerRunOrchestrator skeleton + WorkerRunController**
  - Cluster: Run lifecycle
  - Spec refs: `spec: worker-run-lifecycle/spec.md`
  - Design refs: `design §13 (11 ordered steps)`; `design §6.1`
  - Doc refs: `doc §11 CU1 L963-991`
  - Acceptance:
    - **WorkerRunOrchestrator:** `startRun(ctx, workerId, objective)` creates RUNNING run, emits `WORKER_RUN_CREATED` via T0's `AuditRecorder`; resolves terminate state per spec (COMPLETED/FAILED/ESCALATED/BLOCKED). Ports injected: `PolicyEngine`, `ToolGateway`, `LlmClient`, `EvidenceValidator`, `EscalationService`, `AuditRecorder`. Inline step-order test asserts recorded steps match happy path sequence.
    - **WorkerRunController:**
      - `POST /api/v1/tenants/{tenantId}/workers/{workerId}/runs`
      - Contains **no business logic** — delegates entirely to `WorkerRunOrchestrator`.
      - Minimal response body:
        ```json
        { "runId": "…", "state": "RUNNING", "createdAt": "…", "message": "…" }
        ```
        `message` is present only when the run is immediately BLOCKED or ESCALATED (e.g. kill switch active, tenant paused).
      - **Role of `{tenantId}` path param = SCOPE/CHECK, NOT source of authority.** The controller resolves the authoritative tenant from `TenantSecurityContextResolver` (T3) and then **verifies** it equals the path `{tenantId}`. Mismatch → HTTP 403 (`FORBIDDEN_TENANT_ACCESS`) + `CLIENT_SUPPLIED_TENANT_REJECTED` audit. The path value is never trusted on its own to scope a query or load a tenant.
  - Deps: T6, T3, T0 (AuditRecorder)
  - Size est: ~260 lines

## Phase 4: Document corpus (cluster 4)

- [ ] **T8 — DocumentSource + DocumentChunk + search (Phase 1 ILIKE/FTS)**
  - Cluster: Document corpus
  - Spec refs: `spec: document-corpus-management/spec.md`
  - Design refs: `design §12 (document_source, document_chunk)`
  - Doc refs: `doc §7 RF4 L396-440`; `doc §14 Fase 1 L1229-1245`
  - Acceptance: ILIKE + `to_tsvector('simple', content)` GIN index search **only** (no pgvector dependency in Phase 1); soft delete (`deleted_at`) excludes results; cross-tenant query returns empty; tests cover same-tenant hit + cross-tenant miss.
  - Deps: T7
  - Size est: ~150 lines

## Phase 5: ToolGateway + document.search (cluster 5)

- [ ] **T9 — ToolGateway port + ToolCost + UNAUTHORIZED_TOOL signal**
  - Cluster: Tool gateway
  - Spec refs: `spec: tool-gateway-and-document-search/spec.md`
  - Design refs: `design §3 (tool)`; `design §10`
  - Doc refs: `doc §7 RF5 L442-486`; `doc §7 RF13 L749-769`
  - Acceptance:
    - `ToolGateway.execute(tool, input)` returns `{results, toolCost, latency}`; `toolCost` non-zero even from Fake adapter.
    - Unauthorized tool name → emit **`UNAUTHORIZED_TOOL` signal** (produces a signal record, does NOT decide BLOCK/ALLOW itself).
    - PolicyEngine (T15) receives `UNAUTHORIZED_TOOL` and translates to BLOCK/ALLOW per P-003.
  - Deps: T7
  - Size est: ~90 lines

- [ ] **T10 — DocumentSearchTool adapter**
  - Cluster: Tool gateway
  - Spec refs: `spec: tool-gateway-and-document-search/spec.md`
  - Design refs: `design §3 (DocumentSearchTool)`; `design §13 step 4`
  - Doc refs: `doc §7 RF5 L442-486`
  - Acceptance: only registered tool `document.search`; results tenant-scoped + non-soft-deleted; emits `TOOL_CALLED` audit with cost in metadata via T0's `AuditRecorder`; cross-tenant query returns empty (test).
  - Deps: T9, T8, T0 (AuditRecorder)
  - Size est: ~110 lines

## Phase 6: LlmClient contract (cluster 6)

- [ ] **T11 — LlmClient port + Fake/Provider adapters + real cost (raw output only)**
  - Cluster: LLM client
  - Spec refs: `spec: llm-client-contract/spec.md`
  - Design refs: `design §3 (llm)`; `design §10`
  - Doc refs: `doc §7 RF6 L488-518`; `doc §8 RNF7 L899-908`; `doc §12 CL16 L1122-1126`
  - Acceptance:
    - `LlmClient.generate(prompt)` returns **only**:
      - `rawOutput` (String) — the unprocessed text returned by the provider.
      - `inputTokens`, `outputTokens` (integers).
      - `cost` (decimal) — **NEVER 0** for real executions; Fake adapter returns a representative non-zero unit price (decision #4).
      - `latency` (milliseconds).
      - `providerMetadata` (map for provider-specific diagnostic info).
    - **`LlmClient` does NOT return `StructuredAnswer`** — it has no knowledge of the answer schema. Parsing and validation are T12/T13's responsibility.
    - Provider unavailable or timeout → `RUN_FAILED` with `failure_reason` + `provider_error` (NOT HumanEscalation) per RUN_FAILED/ESCALATED split.
    - `LlmCall` row persisted; `LLM_CALLED` audit via T0's `AuditRecorder` with tokens+cost.
  - Deps: T9, T0 (AuditRecorder)
  - Size est: ~150 lines

## Phase 7: StructuredAnswer contract (cluster 7)

- [ ] **T12 — StructuredAnswer JSON schema + parser (rawOutput → validation)**
  - Cluster: Structured answer
  - Spec refs: `spec: structured-answer-contract/spec.md`
  - Design refs: `design §5`
  - Doc refs: `doc §7 RF7 L520-551`
  - Acceptance:
    - Parser accepts `rawOutput` from T11 and produces a validated `StructuredAnswer` per §5 schema.
    - Non-JSON or schema-invalid `rawOutput` → `requiresEscalation=true, escalationReason="invalid_answer"` (never auto-FAILED per decision #5).
    - Empty `sources` → cannot COMPLETE.
    - Non-empty `unsupportedClaims` / `detectedContradictions` flagged.
    - Produces **`INVALID_ANSWER` signal** when parsing fails — T15 (PolicyEngine) translates to BLOCK/ESCALATE per P-006.
  - Deps: T11
  - Size est: ~140 lines

- [ ] **T13 — StructuredAnswerService**
  - Cluster: Structured answer
  - Spec refs: `spec: structured-answer-contract/spec.md`
  - Design refs: `design §5`; `design §6.2`
  - Doc refs: `doc §7 RF7 L520-551`
  - Acceptance:
    - Service exposes `parse(rawOutput)` returning validated `StructuredAnswer`.
    - Parser failure → emits **`INVALID_ANSWER` signal** (does NOT decide ESCALATE itself).
    - Explicit malformed-payload test asserts `escalationReason="invalid_answer"` in the signal.
  - Deps: T12
  - Size est: ~80 lines

## Phase 8: Evidence validation (cluster 8)

- [ ] **T14 — EvidenceValidator (CL1-CL15) — produces signals**
  - Cluster: Evidence
  - Spec refs: `spec: evidence-validation/spec.md`
  - Design refs: `design §13 step 8`
  - Doc refs: `doc §7 RF8 L554-581`; `doc §12 CL5/CL9 L1117-1126`
  - Acceptance:
    - Validates: same-tenant, non-deleted, non-duplicate, non-contradicting, backs-main-claim.
    - Produces **deterministic signals** — does NOT decide BLOCK/ESCALATE itself:
      - Cross-tenant evidence → **`TENANT_ISOLATION_RISK`** signal.
      - Insufficient evidence → **`INSUFFICIENT_EVIDENCE`** signal.
      - Contradicting evidence → **`CONTRADICTION_DETECTED`** signal.
      - Unsupported claim → **`UNSUPPORTED_CLAIM`** signal.
    - `Evidence` rows persisted; `EVIDENCE_VALIDATED` audit emitted via T0's `AuditRecorder`.
    - PolicyEngine (T15) receives signals and translates to BLOCK/ESCALATE per P-004, P-005, P-007, P-010.
  - Deps: T13, T0 (AuditRecorder)
  - Size est: ~180 lines

## Phase 9: PilotValidationConfig (cluster 9 — moved up, BEFORE policy)

- [ ] **T23 — PilotValidationConfig entity + service (RVB1-3)**
  - Cluster: Pilot
  - Spec refs: `spec: pilot-validation-config/spec.md`
  - Design refs: `design §4 (PilotValidationConfig)`; `design §16`
  - Doc refs: `doc §6 RVB1-3 L207-243`
  - Acceptance:
    - **DB-level CHECK constraints (composite):**
      - `real_questions IS NOT NULL`
      - `jsonb_typeof(real_questions) = 'array'`
      - `jsonb_array_length(real_questions) >= 10`
      - Combined as: `CONSTRAINT chk_real_questions CHECK (real_questions IS NOT NULL AND jsonb_typeof(real_questions) = 'array' AND jsonb_array_length(real_questions) >= 10)`
    - **Service-level validation:** each element of `real_questions` must be a JSONB object with required keys (`question`, `expected_answer`, `source_ref`); reject on structural failure before insert. Design §12 already specifies the table DDL; this task strengthens the constraint beyond a bare length check.
    - Fields: `success_metric{target,unit}`, `stop_criterion{condition,threshold}`, `cost_threshold`.
    - Per-tenant gating (no global flag) — entity/config, not just markdown (decision #12).
    - **Must be available BEFORE:**
      - `CostThresholdChecker` (T21) — reads `cost_threshold`.
      - `PolicyEngine` P-012 (T15) — stop-criterion check.
      - RF14 classification gated (T24) — checks pilot gate.
      - RF15 extraction gated (T25) — checks pilot gate.
      - RVB1-3 validation (cross-cutting).
  - Deps: T1
  - Size est: ~120 lines

## Phase 10: PolicyEngine (cluster 10)

- [ ] **T15 — PolicyRuleRegistry P-001..P-012 + ordered evaluation (decides based on signals)**
  - Cluster: Policy
  - Spec refs: `spec: policy-engine/spec.md`
  - Design refs: `design §7 (rule table)`
  - Doc refs: `doc §7 RF9 L582-643`; `doc §1.2 L56-62`
  - Acceptance:
    - Rules evaluated in priority order §7 table; first triggered short-circuits to its decision.
    - **T15 is the single point where signals → decisions:**
      - `UNAUTHORIZED_TOOL` (from T9) → P-003: BLOCK.
      - `INVALID_ANSWER` (from T13) → P-006: ESCALATE default, BLOCK on clear bypass, always AuditEvent (decisions #5/#7).
      - `TENANT_ISOLATION_RISK` (from T14) → P-005: BLOCK.
      - `INSUFFICIENT_EVIDENCE` (from T14) → P-004: ESCALATE.
      - `CONTRADICTION_DETECTED` (from T14) → P-007: ESCALATE.
      - `UNSUPPORTED_CLAIM` (from T14) → P-010: ESCALATE.
    - P-012 cost threshold reads `PilotValidationConfig.cost_threshold` (T23) for stop-criterion.
    - Earlier components (T9, T13, T14) produce **deterministic signals and validation reasons**; T15 converts them into final policy decisions.
  - Deps: T14, T23
  - Size est: ~280 lines

- [ ] **T16 — HybridDetector (P-007/P-008/P-009)**
  - Cluster: Policy
  - Spec refs: `spec: policy-engine/spec.md`
  - Design refs: `design §7 (Hybrid detection wiring)`
  - Doc refs: `doc §7 RF9 P-007/P-008/P-009 L625-640`; `doc §1.2 L56-62`
  - Acceptance: regex/pattern signals always run; `LlmJudgeClient.suggest()` only suggests; merge `deterministic_signal OR llm_suggestion`; deterministic override wins → BLOCK/ESCALATE per §7 table; LLM-only suggestion → ESCALATE (P-007/P-008 inconclusive → ESCALATE; P-009 inconclusive → ESCALATE); both signals recorded in AuditEvent metadata via T0's `AuditRecorder`.
  - Deps: T15, T0 (AuditRecorder)
  - Size est: ~160 lines

- [ ] **T17 — PolicyEvaluation persistence + POLICY_CHECKED audit**
  - Cluster: Policy
  - Spec refs: `spec: policy-engine/spec.md`; `spec: audit-traceability/spec.md`
  - Design refs: `design §8 (POLICY_CHECKED)`
  - Doc refs: `doc §7 RF9 L582-643`; `doc §7 RF11 L695-729`
  - Acceptance: `PolicyEvaluation` row per run with `rules_evaluated[]`, `decision`, `reasons[]`; each gate emits `POLICY_CHECKED` with `policy_version`, `policy_decision`, `result` via T0's `AuditRecorder`.
  - Deps: T15, T0 (AuditRecorder)
  - Size est: ~120 lines

## Phase 11: HumanEscalation (cluster 11)

- [ ] **T18 — EscalationService + SLA + BusinessCalendar stub** `[decision-pending: #4]`
  - Cluster: Escalation
  - Spec refs: `spec: human-escalation/spec.md`
  - Design refs: `design §11`
  - Doc refs: `doc §7 RF10 L644-693`; `doc §11 CU7 L1082-1101`
  - Acceptance:
    - `assigned_to = tenant.default_escalation_owner || "UNASSIGNED"` → `configuration_required` audit when UNASSIGNED (decision #11).
    - `sla_due_at = SlaCalculator(level)` HIGH=4h/MED=1d/LOW=2d laborables (decision #8).
    - **BusinessCalendar defaults (ship with):**
      - Timezone: `Europe/Madrid`.
      - Business hours: 09:00–18:00.
      - Business days: Monday–Friday.
      - Holidays: **ignored in MVP** (stubbed; real regional calendar provider is Phase 2).
    - **SLA breach model:**
      - When SLA expires: `status = EXPIRED`, `audit_event = SLA_BREACHED` (distinct status and event — not conflated).
      - `breach_reason = SLA_BREACHED` stored in `HumanEscalation.breach_reason`.
    - `HUMAN_ESCALATION_CREATED` audit emitted via T0's `AuditRecorder`.
    - Close guard prevents `WorkerRun.COMPLETED` while escalation PENDING/IN_REVIEW/EXPIRED.
  - **Update `design §17 open question #4` when a real regional provider is confirmed.**
  - Deps: T17, T3, T0 (AuditRecorder)
  - Size est: ~190 lines

- [ ] **T19 — SlaBreachChecker + review controller**
  - Cluster: Escalation
  - Spec refs: `spec: human-escalation/spec.md`
  - Design refs: `design §11` (SlaBreachChecker + review)
  - Doc refs: `doc §7 RF10 L644-693`
  - Acceptance: scheduled job + on-access check: `now > sla_due_at` AND status∈{PENDING,IN_REVIEW} → `EXPIRED` + `SLA_BREACHED` audit; `PATCH /api/v1/tenants/{tid}/human-escalations/{eid}` PENDING→IN_REVIEW→APPROVED|REJECTED|RESOLVED emits `HUMAN_ESCALATION_RESOLVED`, populates `reviewed_at`, `resolved_at`.
  - Deps: T18
  - Size est: ~120 lines

## Phase 12: AuditTraceability (cluster 12 — completes T0's 7-event foundation)

- [ ] **T20 — AuditEvent full 12-event catalog (completes T0)**
  - Cluster: Audit
  - Spec refs: `spec: audit-traceability/spec.md`
  - Design refs: `design §8 (event catalog)`
  - Doc refs: `doc §7 RF11 L695-729`; `doc §1.3 L64-79`
  - Acceptance:
    - `AuditEventType` enum covers all **12 events** (extends T0's 7 initial events with the remaining 5).
    - `JpaAuditRecorder` enriched with all event-specific validations.
    - Mandatory/nullable fields per §8 table; `metadata` JSONB.
    - Cost-bearing events (`LLM_CALLED`, `TOOL_CALLED`) carry non-zero `tokens`/`cost` (decision #4).
    - One test asserts all 12 event types persist + are queryable by `(tenant_id, worker_run_id)`.
    - **T20 is catalog completion** — the recorder itself exists since T0, and T0's 7 events are already in use by earlier tasks.
  - Deps: T17
  - Size est: ~140 lines

## Phase 13: Cost-and-observability (cluster 13)

- [ ] **T21 — CostAggregator + CostThresholdChecker (P-012)**
  - Cluster: Cost
  - Spec refs: `spec: cost-and-observability/spec.md`
  - Design refs: `design §10`
  - Doc refs: `doc §8 RNF3 L811-830`; `doc §8 RNF7 L899-908`
  - Acceptance:
    - `WorkerRun.estimated_cost` accumulates per priced step (`LlmCall.cost` + `ToolCall.cost`).
    - `CostThresholdChecker` recomputes after each priced step against **`PilotValidationConfig.cost_threshold`** (T23) and triggers P-012 (`COST_LIMIT_EXCEEDED`) via PolicyEngine.
    - Never stub-zero (decision #4).
    - **Depends on T23:** PilotValidationConfig must exist before this task; T23 moved to phase 9 to guarantee availability.
  - Deps: T11, T15, T23
  - Size est: ~120 lines

## Phase 14: Data-minimization-and-retention (cluster 14)

- [ ] **T22 — DeletionService + ChunkInvalidator**
  - Cluster: Minimization
  - Spec refs: `spec: data-minimization-and-retention/spec.md`
  - Design refs: `design §3 (minimization)`
  - Doc refs: `doc §8 RNF2 L792-809`
  - Acceptance:
    - Audit stores summaries, never full prompts (RNF2).
    - `DeletionService` soft-deletes `DocumentSource` + chunks (`deleted_at`).
    - Retention policy applied per spec.
    - **`ChunkInvalidator` invalidates derived summaries, search indexes, and evidence references associated with deleted chunks.** It does NOT re-summarize deleted content.
    - Tests assert no full prompt text persisted in `audit_event.metadata`.
  - Deps: T20
  - Size est: ~110 lines

## Phase 15: RF14 classification + RF15 field extraction (cluster 15, optional)

- [ ] **T24 — ClassificationService + ClassificationOutput** (progressive-optional, gated)
  - Cluster: Classification
  - Package: `com.leovinci.aiworkers.classification` (not `classifyopt` — opcionalidad depende de `PilotValidationConfig`, no del nombre del package)
  - Spec refs: `spec: document-classification-rf14/spec.md`
  - Design refs: `design §13 step 3`
  - Doc refs: `doc §7 RF14 (implied §7 RF3 L349-395)`
  - Acceptance: only runs when `PilotValidationConfig` gates ON; invalid output → `INVALID_ANSWER` signal → P-006 via T15; output uses StructuredAnswer shape minus `answer`; sources+validation+audit always present (decision #1); else `WorkerStep(SKIPPED)`.
  - Deps: T23 (pilot gate), T13 (StructuredAnswer parser reuse), T15 (PolicyEngine translates the INVALID_ANSWER signal into a BLOCK/ESCALATE decision)
  - Size est: ~130 lines

- [ ] **T25 — ExtractionService + ExtractionRecord** (progressive-optional, gated)
  - Cluster: Extraction
  - Package: `com.leovinci.aiworkers.extraction` (not `extractopt` — opcionalidad depende de `PilotValidationConfig`, no del nombre del package)
  - Spec refs: `spec: field-extraction-rf15/spec.md`
  - Design refs: `design §13 step 6`
  - Doc refs: `doc §7 RF15 (implied §7 RF3 L349-395)`
  - Acceptance: gated ON by `PilotValidationConfig`; missing values → `extraction_status`, `human_review_required`, `risk_level` (never invented data); `field_sources[]` populated; SKIPPED step when gated off; any policy-relevant anomaly flows through T15 (`PolicyEngine`).
  - Deps: T23 (pilot gate), T13 (StructuredAnswer shape reuse), T15 (PolicyEngine handles any signal raised during extraction)
  - Size est: ~150 lines

## Phase 16: Integration tests (cluster 16)

- [ ] **T26 — Integration suite (Flyway + isolation + audit + cost)**
  - Cluster: Integration
  - Spec refs: all 17 specs (verification gate)
  - Design refs: `design §15`
  - Doc refs: `doc §1.4 L81-86`; `doc §16 L1441-1473`
  - Acceptance: Testcontainers PostgreSQL proving (a) Flyway V2 applies onto V1; (b) NULL `tenant_id` rejected on every tenant-scoped table; (c) two-tenant isolation: tenant A run cannot read tenant B documents/steps/audits/escalations (mandatory per §1.4 L85); (d) full 12-event audit stream recorded across happy path; (e) cost propagation non-zero end-to-end (Fake LLM + Fake Tool both returning representative unit prices).
  - Deps: T17 (policy), T18 (escalation), T20 (audit complete), T21 (cost), T22 (minimization) — integration suite asserts the full stack end-to-end and therefore must run after the last core infrastructure task.
  - Size est: ~240 lines

## Phase 17: Supersede chore (cluster 17)

- [ ] **T27 — Supersede prior `DocumentalWorkers`**
  - Cluster: Supersede
  - Spec refs: `proposal.md "Supersedes"`
  - Design refs: `design §14`
  - Doc refs: `doc §0 L18-40`
  - Acceptance: prior `openspec/changes/DocumentalWorkers/` folder left intact for traceability but marked superseded with a `SUPERSEDED-BY.md` pointer to `DocumentalWorkerMVP-v2`; archive gated by a successful v2 verify-report (late task — apply AFTER `sdd-verify` returns PASS).
  - Deps: verify PASS
  - Size est: ~10 lines

---

## Open Task Questions (genuine, not deferred flags)

- **Q1**: `RUN_FAILED` (infra) vs `ESCALATED` (business) split — **CONFIRMED per user**. RUN_FAILED+AuditEvent for infra failures; ESCALATED+HumanEscalation for business/uncertainty. Apply directly in T6, T11, T15, T18.
  - Source refs: `design §17 question #5`; `doc §12 CL16 L1122-1126`
- **Q2**: pgvector deferred to Fase 2 — confirm T8 ships ILIKE + `to_tsvector('simple', content)` only, with NO pgvector dependency added to the build.
  - Source refs: `doc §14 Fase 1 L1229-1245`, `design §12 comment`
  - **Resolution:** Yes, ship Phase 1 ILIKE + GIN `to_tsvector` only. No new dependency.
- **Q3 (deferred):** JWT claim name for `tenant_id` — safe MVP default in T3 (`app.auth.tenant-claim` = `tenant_id`). Coordinate with IdP before non-dev rollout.
- **Q4 (deferred):** BusinessCalendar real provider (regional holidays) — safe MVP default in T18 (Europe/Madrid, 09:00-18:00, Mon-Fri, no holidays). Phase 2 concern.

No other open questions. Deferred `#3` (JWT claim) and `#4` (BusinessCalendar) are flags baked into T3 and T18 with safe MVP defaults — not re-asked.

## Notes for the apply executor

- Language convention: technical artifacts in English; Spanish only for orchestrator-facing prose, never inside tasks/code.
- Commit by work unit per `work-unit-commits`: each task's commit includes its own tests; no `add all tests` mega-commit.
- Reused-foundation code: refactor in place — `PersistenceEntities` split per capability package, `PolicyEngine` decomposed into `PolicyRuleRegistry` + P-001..P-012 + `HybridDetector`, `AuditRecorder` becomes `JpaAuditRecorder` (12-event port), `LlmClient` returns `rawOutput` (not `StructuredAnswer`).
- `size:exception` PR → still apply commit-by-commit per work unit; one PR at end, not chained.
- `chained_pr_strategy: ask-always` is overridden by the user-approved size exception for this change only.
- **Package naming:** `classifyopt` → `classification`, `extractopt` → `extraction`. Optionality is governed by `PilotValidationConfig`, not package name.
