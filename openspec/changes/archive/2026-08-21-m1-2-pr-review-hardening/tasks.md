# Tasks: M1.2 PR Review Hardening

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | 180–280 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Four independently authorized RED-only slices |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|---|---|---|---|---|---|
| H1 | Atomic idempotency evidence | PR 1 | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#concurrentRegistrationsWithSameTenantAndKeyCreateOneLogicalOperation test` | N/A — in-memory unit | H1 test and evidence |
| H2 | Stored-identity RED evidence | PR 2 | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#findsExpiredStoredWorkByTenantAndKeyWithoutMakingItPending test` | N/A — in-memory unit | H2 test and evidence |
| H3 | Derived-status RED evidence | PR 3 | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#derivesEffectiveStatusWithoutMutatingDeliveryState test` | N/A — in-memory unit | H3 test and evidence |
| H4 | Single-clock-snapshot RED evidence | PR 4 | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#usesOneClockSnapshotForEveryPendingWorkEvaluation test` | N/A — in-memory unit | H4 test and evidence |

## Jira → SDD → Test Mapping

| Jira | SDD requirement/scenario | Intended test |
|---|---|---|
| LCIA-23 H1 | Atomic registration / concurrent equivalents | `concurrentRegistrationsWithSameTenantAndKeyCreateOneLogicalOperation` |
| LCIA-23 H2 | Stored identity / expired identity vs pending | `findsExpiredStoredWorkByTenantAndKeyWithoutMakingItPending` |
| LCIA-23 H3 | Derived effective status / pending expiration | `derivesEffectiveStatusWithoutMutatingDeliveryState` |
| LCIA-23 H4 | Single pending-query time snapshot / advancing clock | `usesOneClockSnapshotForEveryPendingWorkEvaluation` |

## Independently Authorized RED-Only Slices

For **each** H1–H4: natively acquire a new slice attempt (record its supplied source binding); write **only** the named focused test in `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java`; run **only** its literal command above; capture command, UTC start/end timestamp, exit code, expected/actual result, causality, and native source binding in apply evidence; then **STOP before Green**. A passing test is characterization/regression: record it as such, do not fabricate RED/Green, and stop.

### Phase 1: H1 — Atomic idempotency

- [x] 1.1 `concurrentRegistrationsWithSameTenantAndKeyCreateOneLogicalOperation`: preserve H1-RED (three operations/works; next `operation-4`), classify the first Green attempt as **INCONCLUSIVE / TEST ORACLE DEFECT**, then correct only assertion order and record canonical Green proving one concurrent identity, one pending logical `Work`, and next `operation-2`.

### Phase 2: H2 — Stored identity

- [x] 2.1 `expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable`: H2-RED proves expired stored identity is found for its tenant/key, excluded from pending delivery, and hidden from another tenant; executed once and stopped before Green.

### Phase 3: H3 — Effective status

- [x] 3.1 Add `EffectiveStatus` and derive it from a captured clock instant without mutating stored `DeliveryState`; preserve the focused canonical Green evidence and stop before H4.

### Phase 4: H4 — Query time snapshot

- [ ] 4.1 Acquire H4; add only `usesOneClockSnapshotForEveryPendingWorkEvaluation`, proving an advancing clock cannot alter multi-work pending eligibility after the initial read; execute, preserve evidence, and stop before Green.

No task covers M1.3, HTTP/API, persistence, delivery, leases, ACKs, redelivery, or the blocked predecessor/native attempt 15.
