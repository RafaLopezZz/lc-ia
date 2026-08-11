# Policy Engine Specification

Capability: `policy-engine`
Description: The deterministic `PolicyEngine` that decides `ALLOW`, `BLOCK`, or `ESCALATE`. All 12 rules (P-001..P-012) are MVP-mandatory. The LLM never decides; `confidence` never decides.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning deterministic policy gates.

> Source: enriched doc §1.2 (L56-62); §7 RF9 (L582-641); §9 (L912-927). Decisions #3 (P-001..P-012 full MVP), #5 (invalid JSON → ESCALATE), #6 (hybrid regex+LLM judge, deterministic override wins), #7 (P-006 BLOCK/ESCALATE branches + AuditEvent both).

## ADDED Requirements

### Requirement: Deterministic PolicyEngine

The `PolicyEngine` MUST apply deterministic rules and return `{ decision, policyVersion, reasons, requiredAction }` with `decision` ∈ {`ALLOW`, `BLOCK`, `ESCALATE`}. LLM `confidence` MUST NOT decide outcomes (§1.2). Rule inputs include `tenantId`, `workerId`, `workerStatus`, `requestedTool`, `allowedTools`, `riskLevel`, `evidenceStatus`, `hasContradictions`, `hasSensitiveData`, `hasPromptInjectionSignal`.

#### Scenario: Deterministic decision ignores LLM confidence

- GIVEN the LLM reports high confidence but evidence is insufficient
- WHEN policy evaluates
- THEN the decision is `ESCALATE` (P-004)
- AND the LLM confidence is not consulted
> Source: doc §1.2 (L56-62); §7 RF9 (L614-612).

### Requirement: All Twelve Rules Are MVP-Mandatory

The MVP MUST implement all of P-001 through P-012. This resolves the doc inconsistency that labelled P-007+ as "reglas mínimas primer corte" — per decision #3, all twelve are MVP-mandatory.

| Rule | Condition | Decision |
|---|---|---|
| P-001 | Worker `PAUSED` or `DISABLED` | `BLOCK` |
| P-002 | Tenant `PAUSED` or `DISABLED` | `BLOCK` |
| P-003 | Requested tool not in `allowed_tools` | `BLOCK` |
| P-004 | Insufficient evidence | `ESCALATE` |
| P-005 | Source from another tenant | `BLOCK` |
| P-006 | Invalid JSON in structured output | `ESCALATE` (invalid_answer) — BLOCK branch per #7 when clear bypass attempt |
| P-007 | Documentary contradiction | `ESCALATE` |
| P-008 | Sensitive data detected | `ESCALATE` |
| P-009 | Prompt injection detected | `BLOCK` or `ESCALATE` |
| P-010 | High risk | `ESCALATE` |
| P-011 | Irreversible action | `BLOCK` |
| P-012 | Estimated cost exceeds threshold | `ESCALATE` or `BLOCK` |

#### Scenario: Each rule has a unit test

- GIVEN each rule P-001..P-012
- WHEN its trigger condition is met in isolation
- THEN the documented decision is produced
- AND the decision is reproducible across runs
> Source: doc §7 RF9 (L622-640); decision #3.

### Requirement: P-006 Branches (BLOCK / ESCALATE) with AuditEvent on Both

P-006 invalid JSON MUST always ESCALATE with reason `invalid_answer` by default (decision #5 — never an automatic `FAILED` parser branch). However, when the invalid output is a clear attempt to modify rules, access other tenants, run unauthorized tools, or ignore policies, P-006 MUST `BLOCK` (decision #7). On inconclusive suspicion, P-006 MUST `ESCALATE`. BOTH branches MUST emit an `AuditEvent`.

#### Scenario: P-006 ESCALATE branch (inconclusive)

- GIVEN the LLM returns malformed JSON with no clear bypass attempt
- WHEN P-006 evaluates
- THEN the decision is `ESCALATE` with reason `invalid_answer`
- AND an `AuditEvent` is emitted
> Source: doc §7 RF9 P-006 (L629); §12 CL10 (L1116); decisions #5, #7.

#### Scenario: P-006 BLOCK branch (clear bypass)

- GIVEN the LLM output attempts to access another tenant or run an unauthorized tool
- WHEN P-006 evaluates
- THEN the decision is `BLOCK`
- AND an `AuditEvent` of type `RUN_BLOCKED` is emitted
> Source: doc §7 RF9 (L629); §12 CL5/CL9 (L1111, L1115); decision #7.

### Requirement: Hybrid Detection with Deterministic Override (P-007/P-008/P-009)

P-007 (contradiction), P-008 (sensitive data), and P-009 (prompt injection) MUST use a hybrid detection mechanism: regex/pattern heuristics plus an LLM judge that SUGGESTS signals. The `PolicyEngine` MUST always be able to deterministically override the LLM judge. Deterministic override wins.

#### Scenario: Deterministic override wins over LLM judge

- GIVEN the LLM judge flags no injection but a regex pattern detects an injection signature
- WHEN P-009 evaluates
- THEN the deterministic detection result wins
- AND the decision reflects the deterministic signal (BLOCK or ESCALATE)
> Source: doc §1.2 (L56-62); §7 RF9 (L635-637); decision #6.

### Requirement: Risk Taxonomy Drives Defaults

Risk levels follow §9: `LOW` may complete with sufficient evidence; `MEDIUM` may complete only with a direct, unambiguous source; `HIGH` MUST escalate (P-010); `CRITICAL` MUST block (P-011).

#### Scenario: HIGH risk always escalates

- GIVEN a run whose `riskLevel` is `HIGH`
- WHEN P-010 evaluates
- THEN the decision is `ESCALATE` regardless of evidence sufficiency
> Source: doc §9 (L912-927); §7 RF9 P-010/P-011 (L638-639).
