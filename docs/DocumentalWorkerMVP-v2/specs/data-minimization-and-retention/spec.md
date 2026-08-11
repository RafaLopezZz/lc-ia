# Data Minimization and Retention Specification

Capability: `data-minimization-and-retention`
Description: RGPD-aligned minimization, soft delete, deletion audit, and chunk invalidation on document deletion.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning data minimization. Kept separate from cost-and-observability per orchestrator instruction.

> Source: enriched doc §8 RNF2 (L792-808).

## ADDED Requirements

### Requirement: Soft Delete and Deletion Audit

The system MUST use logical deletion (`deleted_at`) for `DocumentSource` and `DocumentChunk`. Every deletion MUST be auditable. Hard delete of tenant-scoped content is a non-goal of the MVP.

#### Scenario: Deletion is soft and audited

- GIVEN an active document
- WHEN it is deleted
- THEN `deleted_at` is set and `status` becomes `DELETED`
- AND a deletion audit event is recorded
> Source: doc §8 RNF2 (L798, L800).

### Requirement: No Full Sensitive Prompts Stored

The system MUST NOT store full prompts when they contain sensitive information. Summaries MUST be stored instead where sufficient for audit.

#### Scenario: Sensitive prompt is summarized

- GIVEN an LLM prompt containing sensitive tenant data
- WHEN persisting the `AI_CALL` step and `LLM_CALLED` event
- THEN only a summary is stored
- AND the full prompt is discarded
> Source: doc §8 RNF2 (L801-802); §7 RF3 (L391).

### Requirement: Chunk Invalidation on Document Deletion

When a document is deleted, its indexed chunks MUST be invalidated or reindexed so no answer can cite them.

#### Scenario: Deleted document chunks unusable

- GIVEN a document is soft-deleted
- WHEN a subsequent `document.search` runs
- THEN no chunk of that document is returned
- AND no answer may cite it
> Source: doc §8 RNF2 (L803); §12 CL13 (L1119).

### Requirement: Retention Configurable (Future)

Retention windows MUST be designed as configurable, but concrete retention enforcement is a non-goal of the MVP. The data model MUST NOT preclude future retention policies.

#### Scenario: Retention field exists

- GIVEN the `DocumentSource`/audit schema
- WHEN a future retention policy is applied
- THEN no schema change blocks applying a retention window
> Source: doc §8 RNF2 (L799).
