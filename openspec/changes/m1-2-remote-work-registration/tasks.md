# Tasks: M1.2 Remote Work Registration

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | 280–380 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR; only RED-1 is authorized now |
| Delivery strategy | auto-chain |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|---|---|---|---|---|---|
| 1 | RED-1 evidence only | PR 1 | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#registersPendingWorkBeforeAnyGatewayPoll test` | N/A — framework-neutral in-memory unit test | Delete the new test and its RED evidence only |

## Phase 1: RED-1 Evidence (authorized first apply batch)

- [x] 1.1 Create `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` with `registersPendingWorkBeforeAnyGatewayPoll`; express service-first registration with no poll and assertions for one PENDING record, generated operation ID, and retained tenant, gateway, idempotency, correlation, and expiry fields.
- [x] 1.2 Complete by reference to the existing RED-1 evidence in apply-progress; no evidence is reconstructed here.
- [x] 1.3 Historical stop satisfied: the agent stopped after RED-1. Later RED/GREEN work had separate explicit authorizations; this is not a permanent prohibition.

## Phase 2: Future RED Evidence (not authorized in the first batch)

- [x] 2.1 RED-2: add and execute a focused tenant-scoped idempotency/isolation test for `Reuses only the tenant-scoped operation`; preserve the strict RED record before implementation.
- [x] GREEN-2: implement only `pendingFor(TenantId, GatewayId)` for pre-registered PENDING work matching the requested tenant and target gateway.
- [x] 2.2 RED-3: add and execute a focused tenant-scoped idempotency test for `Reuses only the tenant-scoped operation`; preserve the strict RED record before implementation.
- [x] GREEN-3: implement compatible idempotency reuse keyed exactly by `TenantId` + `IdempotencyKey`.
- [x] 2.3 RED-4: add and execute a controlled-clock expiry test for `Excludes expired work absolutely`; preserve the strict RED record before implementation.
- [x] 2.4 RED-5: add and execute a tenant-scoped correlation lookup test for `Restricts correlation status to its tenant`; preserve the strict RED record before implementation.
- [x] GREEN-5: implement only `statusFor(TenantId, CorrelationId)` with tenant-scoped effective status derived from the injected clock.
- [x] 2.5 Characterization/regression PASS: `Preserves deterministic eligible-work order` passed without a historical RED-6 or production change; no GREEN-6 exists.
- [x] 2.6 GREEN-2.6: complete the validation-only `SyntheticTrustBoundary` regression using the preserved architectural RED and authorized GREEN-2.6 evidence; focal M1.1 regression 1/1, `SyntheticTrustBoundaryTest` 6/6, and HTTP boundary 3/3 pass.
