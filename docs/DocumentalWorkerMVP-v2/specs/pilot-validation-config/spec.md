# Pilot Validation Config Specification

Capability: `pilot-validation-config`
Description: The `PilotValidationConfig` entity encoding RVB1 (real process), RVB2 (success metric), and RVB3 (stop criterion) as persisted configuration, not just markdown.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning pilot validation (none explicit).

> Source: enriched doc §6 (L207-243). Decision #12 (RVB1-3 as entity/config).

## ADDED Requirements

### Requirement: PilotValidationConfig Entity

The system MUST persist a `PilotValidationConfig` per tenant (or per pilot) with fields: `id`, `tenant_id`, `process_description`, `process_owner`, `estimated_current_time`, `real_questions` (≥10), `success_metric` (target + unit), `stop_criterion` (condition + threshold), `cost_threshold`, `status`, `created_at`, `updated_at`. RVB1, RVB2, and RVB3 MUST each map to required fields, not prose.

#### Scenario: Config persisted before pilot

- GIVEN a tenant preparing a pilot
- WHEN a `PilotValidationConfig` is created
- THEN it contains a process description, ≥10 real questions, a success metric, and a stop criterion
- AND the pilot cannot be marked active without all three
> Source: doc §6 RVB1/RVB2/RVB3 (L209-243); decision #12.

### Requirement: RVB1 Real Process Identified

Before a pilot is valid, the config MUST record a concrete repetitive documentary process, who executes it today, approximate time consumed, and ≥10 real questions/tasks.

#### Scenario: Process fields validated

- GIVEN a config missing `process_description` or with fewer than 10 `real_questions`
- WHEN the config is saved
- THEN validation rejects it
- AND the pilot remains `DRAFT`
> Source: doc §6 RVB1 (L209-218).

### Requirement: RVB2 Success Metric Defined

The config MUST define a simple success metric (e.g. reduce search time 30%, answer 8/10 frequent queries correctly with source, escalate 100% of no-evidence queries, cost per run below threshold).

#### Scenario: Metric is queryable

- GIVEN an active pilot config with `success_metric`
- WHEN pilot results are evaluated
- THEN the metric target and unit are available for comparison
> Source: doc §6 RVB2 (L220-230).

### Requirement: RVB3 Stop Criterion Defined

The config MUST define a stop criterion (e.g. cost exceeds estimated savings, >50% queries escalate for lack of documents, users do not reuse the worker, reviewer takes longer than direct resolution, tenant isolation cannot be guaranteed).

#### Scenario: Stop criterion triggers evaluation

- GIVEN a pilot in progress
- WHEN the stop criterion condition is met (e.g. >50% escalation rate)
- THEN a stop recommendation is produced
- AND it feeds the cost-and-observability P-012 pipeline
> Source: doc §6 RVB3 (L232-242); §12 CL11 (L1117).
