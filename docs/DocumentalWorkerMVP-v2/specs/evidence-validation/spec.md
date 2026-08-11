# Evidence Validation Specification

Capability: `evidence-validation`
Description: Validation that an answer is backed by sufficient, same-tenant, non-contradictory, non-deleted, traceable evidence before the run may complete.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning evidence sufficiency.

> Source: enriched doc §7 RF8 (L554-579); §12 CL1-CL15 (L1105-1121).

## ADDED Requirements

### Requirement: Sufficient Evidence Conditions

Evidence is sufficient ONLY when ALL hold: (1) belongs to the same `tenant_id` as the run; (2) has `document_id`; (3) has `chunk_id`; (4) has retrievable text or verifiable excerpt; (5) exceeds a minimum relevance score when a score exists; (6) is not from a deleted document; (7) does not contradict another relevant recovered source; (8) explicitly backs the answer's main claim. A run may be `COMPLETED` only when at least one main claim is backed by a traceable same-tenant source and there are no contradiction/sensitivity/critical-ambiguity signals.

#### Scenario: Sufficient evidence completes

- GIVEN an answer whose main claim is backed by a same-tenant, non-deleted, non-contradicting source with a verifiable excerpt
- WHEN evidence validation runs
- THEN validation passes
- AND the run may proceed to `COMPLETED`
> Source: doc §7 RF8 (L558-571).

### Requirement: Insufficient Evidence Escalates

A query with no results, an answer with no sources, or a source from a deleted document MUST escalate.

#### Scenario: No results escalates

- GIVEN a `document.search` returns no results
- WHEN evidence validation runs
- THEN the run escalates with reason `INSUFFICIENT_EVIDENCE`
- AND no invented answer is returned
> Source: doc §7 RF8 (L575-576); §11 CU2 (L993-1011).

### Requirement: Cross-Tenant Evidence Blocks

A source belonging to another tenant MUST cause a `BLOCK` (policy P-005) and MUST be recorded as a tenant-isolation incident.

#### Scenario: Cross-tenant source blocks

- GIVEN validation finds a source whose `tenant_id` differs from the run's
- WHEN policy evaluates
- THEN the run is `BLOCKED`
- AND a `TENANT_ISOLATION_RISK` incident is audited
> Source: doc §7 RF8 (L577); §7 RF9 P-005 (L628); §12 CL5 (L1111).

### Requirement: Contradiction Escalates

When two relevant recovered sources contradict each other, the system MUST escalate with reason `CONTRADICTORY_SOURCES` and MUST NOT arbitrarily choose one version.

#### Scenario: Contradicting sources escalate

- GIVEN source A and source B state conflicting facts
- WHEN validation detects the contradiction
- THEN the run escalates citing both sources
- AND the LLM confidence is NOT used to choose
> Source: doc §7 RF8 (L578); §11 CU3 (L1015-1029); §12 CL4 (L1110).

### Requirement: Duplicate Documents Are Not Independent Evidence

Duplicate documents MUST NOT be counted as independent evidence (CL3); stale-vs-recent sources with unclear validity MUST escalate (CL2).

#### Scenario: Duplicates do not inflate evidence

- GIVEN two identical duplicates of one document
- WHEN evidence is counted
- THEN they count as a single source
- AND if that single source is insufficient, the run escalates
> Source: doc §12 CL3 (L1109); §12 CL2 (L1108).
