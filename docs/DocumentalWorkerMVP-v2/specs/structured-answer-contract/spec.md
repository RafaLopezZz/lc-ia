# Structured Answer Contract Specification

Capability: `structured-answer-contract`
Description: The `StructuredAnswer` JSON contract and the rules that sourceless, unsupported, contradictory, or invalid answers cannot complete.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning structured output.

> Source: enriched doc §7 RF7 (L520-551); §8 RNF4 (L832-843). Decisions #5 (invalid JSON → ESCALATE invalid_answer), #7 (P-006 branches).

## ADDED Requirements

### Requirement: StructuredAnswer JSON Contract

Every LLM answer MUST be parseable into `{ answer, sources: [ { documentId, chunkId, title, excerptUsed } ], requiresEscalation, escalationReason, riskLevel, unsupportedClaims: [], detectedContradictions: [] }`. Output that affects flow MUST be parseable, validated, rejected if non-compliant, and evaluated by the `PolicyEngine` (RNF4).

#### Scenario: Valid structured answer proceeds

- GIVEN the LLM returns a JSON matching the contract
- WHEN the parser validates it
- THEN the answer proceeds to evidence validation
- AND no tool is triggered by the LLM suggestion alone
> Source: doc §7 RF7 (L526-543); §8 RNF4 (L834-840).

### Requirement: Empty Sources Cannot Complete

If `sources` is empty, the run MUST NOT end as `COMPLETED`; it MUST escalate (policy P-004).

#### Scenario: No sources forces escalation

- GIVEN an LLM answer with `sources: []`
- WHEN policy evaluates
- THEN the run escalates with reason `INSUFFICIENT_EVIDENCE`
- AND no answer is returned to the user
> Source: doc §7 RF7 (L547); §7 RF9 P-004 (L627).

### Requirement: Unsupported Claims or Contradictions Escalate

If `unsupportedClaims` is non-empty or `detectedContradictions` is non-empty, the run MUST escalate.

#### Scenario: Unsupported claims escalate

- GIVEN an answer with one or more `unsupportedClaims`
- WHEN policy evaluates
- THEN the run escalates with reason `UNSUPPORTED_CLAIMS`
- AND the claims are recorded in the escalation
> Source: doc §7 RF7 (L548-549); §12 CL15 (L1121).

### Requirement: Invalid JSON Always Escalates (Never Auto-FAILED)

If the LLM output is invalid JSON that cannot be parsed, the system MUST ALWAYS escalate with reason `invalid_answer`. The system MUST NOT take an automatic `FAILED` branch from the parser (decision #5). Policy P-006 governs the branch: BLOCK on clear attempt to bypass rules / access other tenants / run unauthorized tools / ignore policies; ESCALATE on inconclusive suspicion. Both branches MUST emit an `AuditEvent`.

#### Scenario: Invalid JSON escalates

- GIVEN the LLM returns malformed JSON
- WHEN the parser fails
- THEN the run escalates with reason `invalid_answer`
- AND an `AuditEvent` is emitted
- AND no automatic `FAILED` branch is taken solely from the parse error
> Source: doc §7 RF7 (L550); §7 RF9 P-006 (L629); §12 CL10 (L1116); decisions #5, #7.
