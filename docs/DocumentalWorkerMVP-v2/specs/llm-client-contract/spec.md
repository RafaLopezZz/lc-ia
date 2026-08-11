# LLM Client Contract Specification

Capability: `llm-client-contract`
Description: The decoupled `LlmClient` interface, token/cost/latency recording, and the rule that invalid LLM output never triggers downstream actions.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning LLM interaction.

> Source: enriched doc §7 RF6 (L488-517); §8 RNF5 (L846-855). Decision #4 (real cost from day 1).

## ADDED Requirements

### Requirement: Decoupled LlmClient Interface

The system MUST define an internal `LlmClient` interface (e.g. `StructuredAnswer generateAnswer(DocumentalPrompt prompt)`) and MUST NOT couple the domain directly to any concrete provider. The active provider and model MUST be swappable without domain changes.

#### Scenario: Provider is swappable

- GIVEN a `FakeLlmClient` stub and a real provider implementation
- WHEN the implementation binding is changed
- THEN the domain code is unchanged
- AND both implementations satisfy the contract
> Source: doc §7 RF6 (L496-514); §8 RNF6 (L885-889).

### Requirement: LLM Call Records Cost and Latency

Each `LlmClient` call MUST record `provider`, `model`, estimated `tokens` (when available), estimated `cost`, `latency`, input summary, and output summary. The cost MUST be a real non-zero value when the provider returns tokens/tool cost (decision #4); stub-zero is NOT acceptable.

#### Scenario: Real cost recorded from stub return

- GIVEN a stub `LlmClient` returns tokens=120 and a unit cost
- WHEN the call completes
- THEN the `AI_CALL` step records a non-zero `estimated_cost`
- AND the `AuditEvent` of type `LLM_CALLED` persists tokens and cost
> Source: doc §7 RF6 (L503-510); §8 RNF7 (L899-908); decision #4.

### Requirement: Invalid LLM Output Does Not Execute Tools

If the LLM returns an invalid or unparseable response, the system MUST NOT execute any tool and MUST produce a controlled failure or escalation (see structured-answer-contract and policy-engine P-006).

#### Scenario: Invalid output blocks downstream

- GIVEN the LLM returns malformed JSON
- WHEN the structured-answer parser fails
- THEN no tool is executed
- AND the run escalates with reason `invalid_answer` (decision #5)
> Source: doc §7 RF6 (L515-516); §12 CL10 (L1116); decision #5.

### Requirement: Model Unavailable is Controlled Failure

If the LLM provider is unavailable, the run MUST end as `FAILED` or follow a controlled fallback; it MUST NOT hang or silently succeed.

#### Scenario: Provider unavailable

- GIVEN the configured provider is unreachable
- WHEN the `LlmClient` call fails
- THEN the run ends as `FAILED`
- AND the failure reason records the provider error
> Source: doc §12 CL16 (L1122).
