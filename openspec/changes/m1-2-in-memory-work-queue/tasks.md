# Tasks: M1.2 In-Memory Work Queue

Queue-only, framework-neutral core. Do not modify `SyntheticTrustBoundary` unless a RED test proves a compatibility need; no alternate idempotency ownership.

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 360–460 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single atomic work unit |
| Delivery strategy | auto-chain |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

Forecast is below the supplied 800-line review budget; monitor the 400-line default guard and split only if the actual diff exceeds 800 lines.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Queue contract and minimal core | Single PR | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkQueueTest test` | N/A — no changed transport/runtime boundary | Delete `InMemoryWorkQueue.java` and `InMemoryWorkQueueTest.java` |

## Phase 1: RED Queue Contracts

- [x] 1.1 Create `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkQueueTest.java`; RED A: boundary-accepted work is returned only for its trace target gateway and owning tenant.
- [x] 1.2 In that test, RED B: two boundary accepts for one `(tenant, idempotencyKey)` retain the same Work identity; recording both yields one pending Work, with no queue idempotency map.
- [x] 1.3 In that test, RED C: use `Clock.fixed` and a test-only mutable `Clock`; at exact expiry and after advancement, pending lookup excludes Work and status is explicitly `EXPIRED`.
- [x] 1.4 In that test, RED D: correlation lookup returns `PENDING` for the owning tenant and `Optional.empty()` for another tenant or unknown correlation.
- [x] 1.5 In that test, RED E: interleaved tenant/gateway records return only the requested tenant/target Work in stable acceptance order on repeated queries.

## Phase 2: Minimal Queue Implementation

- [x] 2.1 Create `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkQueue.java` with injected `Clock`, null checks, `record(Accepted)`, `pendingFor(tenant, gateway)`, and `status(tenant, correlation)`.
- [x] 2.2 Implement record deduplication only by `(tenant, operationId)`, route derivation only from `Accepted.trace()`, and monotonic acceptance sequence; retain boundary Work unchanged.
- [x] 2.3 Filter every query by tenant and absolute `expiresAt().isAfter(clock.instant())`; return `PENDING` or explicit `EXPIRED`, never extend, revive, acknowledge, lease, or redeliver Work.

## Phase 3: Verification

- [x] 3.1 Run `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkQueueTest test`; all RED A–E scenarios pass after Phase 2.
- [x] 3.2 Run `mvn -B -pl synthetic-retrieval test`; preserve all M1.1 tests unchanged.
- [x] 3.3 Run root `mvn -B clean verify`; confirm reactor regression succeeds without POM, dependency, server, or transport changes.
