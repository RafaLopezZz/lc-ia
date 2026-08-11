# Field Extraction (RF15) Specification

Capability: `field-extraction-rf15`
Description: Progressive-optional MVP field extraction. Never dropped; always gated behind a real CU, with sources + validation + audit.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning the "MVP Extraction Record" requirement.

> Source: enriched doc §7 RF3 (L355); prior `DocumentalWorkers` spec "MVP Extraction Record"; §17 global acceptance (L1475-1488); proposal decision #1. Note: RF15 is not enumerated as a standalone RF in the enriched doc; it is implied by the extraction step and is encoded here as a progressive-optional capability per decision #1.

## ADDED Requirements

### Requirement: Progressive-Optional and Never Dropped

`field-extraction` is a progressive-optional capability. It MUST NOT be dropped from the roadmap. It MUST be gated behind a real candidate CU recorded in `PilotValidationConfig`. When inactive, the extraction step MUST be recorded as `SKIPPED` in `WorkerStep`, not omitted.

#### Scenario: Inactive extraction is skipped not removed

- GIVEN no real CU requires extraction
- WHEN a run executes
- THEN the extraction `WorkerStep` is `SKIPPED`
- AND the run proceeds without extraction
> Source: doc §6 (L207-243); decision #1.

### Requirement: MVP Extraction Record (When Active)

When extraction is active, the system MUST produce the MVP field set: `document_type`, `provider_or_client`, `dates`, `costs`, `activity`, `priority`, `status`, `source_name`, `source_date`, `extraction_status`, `human_review_required`, `risk_level`. Missing values MUST be represented by `extraction_status`, never by invented data.

#### Scenario: Extraction includes required fields

- GIVEN a supported contract/invoice/delivery note/email/customer document
- WHEN extraction completes
- THEN every MVP extraction field is present
- AND missing values are flagged via `extraction_status`, not fabricated
> Source: prior `DocumentalWorkers` "MVP Extraction Record"; doc §7 RF3 (L355).

### Requirement: Sources, Validation, and Audit When Active

Every extracted field value MUST cite a source chunk (`document_source_id`, `chunk_id`), MUST pass evidence validation (same-tenant, non-deleted, non-contradicting), and MUST emit an `AuditEvent`. Ambiguous or conflicting values MUST set `human_review_required=true` and reflect the ambiguity in `risk_level`.

#### Scenario: Ambiguous value requires review

- GIVEN a document with ambiguous or conflicting field values
- WHEN extraction cannot determine a required value from evidence
- THEN `human_review_required` is true
- AND `risk_level` reflects the ambiguity
- AND the run escalates if evidence is insufficient
> Source: doc §7 RF8 (L554-579); §12 CL1-CL15 (L1105-1121); decision #1.

### Requirement: Invalid Extraction Output Escalates

Invalid extraction output MUST escalate with reason `invalid_answer` (P-006), never auto-FAILED.

#### Scenario: Invalid extraction JSON escalates

- GIVEN extraction returns malformed JSON
- WHEN the parser fails
- THEN the run escalates with reason `invalid_answer`
- AND no automatic `FAILED` branch is taken
> Source: doc §7 RF9 P-006 (L629); §12 CL10 (L1116); decisions #1, #5.
