# Document Classification (RF14) Specification

Capability: `document-classification-rf14`
Description: Progressive-optional document classification capability. Never dropped; always gated behind a real CU, with sources + validation + audit.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning classification (the "Ordered Documentary Run" classification step).

> Source: enriched doc §7 RF3 step ordering (L355); §17 global acceptance (L1475-1488); proposal decision #1. Note: RF14 is not enumerated as a standalone RF in the enriched doc; it is implied by the classification step and is encoded here as a progressive-optional capability per decision #1.

## ADDED Requirements

### Requirement: Progressive-Optional and Never Dropped

`document-classification` is a progressive-optional capability. It MUST NOT be dropped from the roadmap. It MUST be gated behind a real candidate CU (a concrete repetitive documentary process recorded in `PilotValidationConfig`). When inactive, the classification step MUST be recorded as `SKIPPED` in `WorkerStep`, not omitted.

#### Scenario: Inactive classification is skipped not removed

- GIVEN no real CU requires classification
- WHEN a run executes
- THEN the classification `WorkerStep` is `SKIPPED`
- AND the run proceeds without classification
> Source: doc §6 (L207-243); decision #1.

### Requirement: When Active, Always With Sources, Validation, and Audit

When classification is active, every classification result MUST cite source metadata (`document_source_id`, `chunk_id`), MUST pass evidence validation (same-tenant, non-deleted, non-contradicting), and MUST emit an `AuditEvent`. Classification MUST NOT decide policy outcomes; the `PolicyEngine` decides.

#### Scenario: Classification cites sources and is audited

- GIVEN classification is active for a CU
- WHEN a document is classified
- THEN the result cites the source chunk used
- AND an `AuditEvent` records the classification
- AND the classification does not itself allow or block the run
> Source: doc §1.2 (L56-62); §7 RF4 (L396-439); decision #1.

### Requirement: Classification Output Contract

Classification output MUST follow a structured contract: `{ documentId, predictedType, confidence, sources: [ { documentId, chunkId, excerptUsed } ], requiresEscalation, escalationReason }`. Invalid output MUST escalate with reason `invalid_answer` (P-006), never auto-FAILED.

#### Scenario: Invalid classification output escalates

- GIVEN classification returns malformed JSON
- WHEN the parser fails
- THEN the run escalates with reason `invalid_answer`
- AND no automatic `FAILED` branch is taken
> Source: doc §7 RF7 (L550); §7 RF9 P-006 (L629); decisions #1, #5.
