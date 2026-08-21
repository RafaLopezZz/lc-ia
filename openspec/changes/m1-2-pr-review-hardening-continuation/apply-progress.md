# Apply Progress: M1.2 PR Review Hardening Continuation

## Authorized scope

Executed only H3-R under authorization `sha256:4a08ed6cc19ad4908569db7f50ef970c19e0b24929bb49b47697870daa65b481`, then only H4-RED under continuation authorization `sha256:929a1542cb046bd2209d611367dc3e3c3904cbe695e9c20995fee180f43a57f2`, and then only GREEN-H4 under continuation authorization `sha256:d33bc3a4702b96eeb04d9df8993bbb249a782a5a4c51d2c8c191f14096334db2`.

## H3-R evidence

### Read-only inventory

| Reference | Classification | Result |
|---|---|---|
| `DeliveryState.PENDING` / `Work.deliveryState` | Stored lifecycle state | Retained. |
| `EffectiveStatus` / `effectiveStatusFor` | Derived effective state | Retained and used by the correlation consumer and pending filter. |
| `Status` | Legacy parallel state vocabulary | Removed. |
| `statusFor` | Legacy correlation consumer | Removed; migrated to `effectiveStatusFor`. |
| `Work.status` | Legacy stored record component | Removed. |

### GREEN BASELINE — PASS

`mvn -pl synthetic-retrieval "-Dtest=InMemoryWorkRegistryTest#registersPendingWorkBeforeAnyGatewayPoll+storedDeliveryStateRemainsPendingWhileEffectiveStatusExpiresWithClock+returnsStatusByCorrelationIdOnlyWithinTheOwningTenant+expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable+neverDeliversExpiredWorkWithControlledClock" test`

Result: exit 0; 5 tests run, 0 failures, 0 errors, 0 skipped.

Covered: stored `DeliveryState.PENDING`; effective `PENDING` before expiry and `EXPIRED` at expiry; tenant-isolated correlation lookup; expired identity lookup; no expired delivery.

### REFACTOR

Removed `Status`, `statusFor`, and the `Work.status` component. Migrated the package-local correlation test and pending filter to `EffectiveStatus`; `DeliveryState` remains the stored lifecycle state. No H4 snapshot work was performed.

### GREEN REGRESSION — PASS

`mvn -pl synthetic-retrieval "-Dtest=InMemoryWorkRegistryTest#registersPendingWorkBeforeAnyGatewayPoll+storedDeliveryStateRemainsPendingWhileEffectiveStatusExpiresWithClock+returnsEffectiveStatusByCorrelationIdOnlyWithinTheOwningTenant+expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable+neverDeliversExpiredWorkWithControlledClock" test`

Result: exit 0; 5 tests run, 0 failures, 0 errors, 0 skipped.

Absence check: `Status`, `statusFor`, and `Work.status` have no remaining Java references under `synthetic-retrieval`.

## H4-RED evidence — REAL RED

Added only `CountingClock` and `pendingQueryUsesOneClockSnapshot` in `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java`. The fixture returns fixed `T = 2026-08-19T12:00:00Z` and counts `instant()` calls. The test registers three work items for the same tenant and gateway with distinct idempotency keys and correlations, each expiring after `T`; it resets the counter, invokes `pendingFor` once, and asserts three returned items and exactly one clock read.

- Command (executed exactly once): `mvn -pl synthetic-retrieval "-Dtest=InMemoryWorkRegistryTest#pendingQueryUsesOneClockSnapshot" test`
- Result: exit 1; 1 test run, 1 failure, 0 errors, 0 skipped.
- Counter: expected `1`; actual `3` after one `pendingFor` invocation. The returned-item assertion passed at `3`.

## GREEN-H4 — CANONICAL PASS

In `pendingFor(TenantId, GatewayId)`, captured `Instant now = clock.instant()` once before filtering. Every candidate now uses `effectiveStatusFor(work, now)`; expiry retains the required strict `expiresAt > now` behavior through `work.expiresAt().isAfter(now)`.

- Command (executed exactly once): `mvn -pl synthetic-retrieval "-Dtest=InMemoryWorkRegistryTest#pendingQueryUsesOneClockSnapshot" test`
- Result: exit 0; 1 test run, 0 failures, 0 errors, 0 skipped.
- Refactor: none needed; the minimal snapshot capture and existing explicit-instant helper satisfy the behavior.

## Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | H4 GREEN canonical command above executed once and passed: exit 0; 1 test run, 0 failures, 0 errors, 0 skipped. |
| Runtime harness command/scenario and exact result | N/A — this package-local in-memory registry has no runtime boundary beyond its direct JUnit behavioral contract. |
| Rollback boundary | Revert only the H4 snapshot lines in `InMemoryWorkRegistry.java`; the pre-existing H4 test, H3-R, and predecessor/native state are unaffected. |

## Status

H3-R REFACTOR PASS. GREEN-H4 canonical PASS. Tasks 1.1, 1.2, 2.1, 3.1, and 4.1 are complete. Stop for settlement; no native acquire, global test, Git action, or other change was performed.
