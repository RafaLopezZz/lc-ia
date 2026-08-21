# Tasks: Continuación del endurecimiento de revisión M1.2

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | 440–720 total; 220–360 per acquire |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | Slice 1 H3-R → Slice 2 H4 |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Delivery slice | Required coverage | Rollback boundary |
|---|---|---|---|---|
| 1 | H3-R status migration | Slice 1 | Baseline and regression inventory below | H3-R source/test diff |
| 2 | H4 clock snapshot | Slice 2 | Sequential-clock snapshot coverage | H4 source/test diff |

## Phase 1: H3-R GREEN BASELINE

- [x] 1.1 Classify in `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` the GREEN baseline: non-expired `PENDING`, expired/en-limit `EXPIRED`, stored `DeliveryState.PENDING`, correlation lookup, and tenant isolation.
- [x] 1.2 Prove that the complete H3-R baseline inventory is GREEN before changing `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java`.

## Phase 2: H3-R REFACTOR

- [x] 2.1 Remove `Status`, `statusFor`, `Work.status`, and migrate legitimate package-local consumers in `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` without changing the classified baseline behavior.

## Phase 3: H3-R GREEN REGRESSION

- [x] 3.1 Re-prove the complete baseline inventory after refactor and confirm the removed surface is absent; H3-R MUST NEVER have a RED phase.

## Phase 4: H4 RED → GREEN → REFACTOR

- [x] 4.1 Add the sequential-clock characterization in `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java`, then implement and regress one-query snapshot behavior in `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java`.

## Requirement-to-Task Mapping

| Requirement / scenario | Classification | Required coverage |
|---|---|---|
| Consolidated effective state | H3-R GREEN BASELINE + GREEN REGRESSION | All baseline cases, migration, and tenant isolation before and after refactor |
| Single gateway-query snapshot | H4 RED → GREEN → REFACTOR | Shared instant and expiry boundary |

## Evidence Boundary

H3 has a technical PASS and a documented native-settlement exception. This continuation neither inherits nor resets that exception or any predecessor/native state.

This planning phase changes only documentation. It executes no source change, test, command, Git action, or native acquire.
