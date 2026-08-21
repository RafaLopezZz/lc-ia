# Exploration: M1.2 PR Review Hardening

## Current State

LCIA-23 is implemented on `feat/m1-2-in-memory-work-queue` for PR #5. `InMemoryWorkRegistry` remains an in-memory, package-local registry: registration linearly searches an `ArrayList`, then allocates an operation ID and appends a `Work`. This is correct only for serial callers: concurrent same-tenant/same-key registrations can both miss and create distinct operations.

The registry exposes an overloaded `pendingFor(TenantId, IdempotencyKey)` test-oriented lookup, not an explicit identity lookup. It exposes stored `Work.status == PENDING` while `statusFor` independently derives `PENDING` or `EXPIRED` from the clock, which can present contradictory status views after expiry. `pendingFor(TenantId, GatewayId)` calls `clock.instant()` once per candidate, so one query can evaluate different instants.

The predecessor `m1-2-remote-work-registration` remains historical **BLOCKED/maintainer_decision**: native attempt 15 stays running because the native remediation path rejects an evidence-only unchanged candidate. This successor neither changes that state nor attempts to settle it.

### Scope Map

| Hardening item | Required outcome |
|---|---|
| H1 | Make tenant-plus-idempotency registration atomic, preserving one operation identity for concurrent equivalent registrations. |
| H2 | Replace the test-shaped idempotency overload with explicit `findByIdempotency`; identity lookup includes an expired stored record and remains tenant-scoped. |
| H3 | Store delivery lifecycle as `DeliveryState`; derive `EffectiveStatus` from that state and a clock instant rather than storing a competing status field. |
| H4 | Capture exactly one `Clock.instant()` snapshot at the start of every pending query and use it for all expiry checks. |

## Affected Areas

- `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` — atomic registration, explicit identity lookup, state model, and query-time snapshot belong here.
- `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` — characterize concurrent idempotency, expired identity lookup, non-divergent effective status, and a single query snapshot.
- `openspec/changes/m1-2-pr-review-hardening/exploration.md` — records this independent successor's scope and predecessor boundary.

`SyntheticTrustBoundary`, its HTTP adapter, and predecessor artifacts are not affected. Registration remains pre-poll and the trust boundary remains validation-only, without Work creation.

## Approaches

1. **Synchronized registry with explicit read models** — synchronize the small `register` critical section; retain ordered storage; add `findByIdempotency`; derive `EffectiveStatus` using an injected/captured instant; snapshot time once in `pendingFor`.
   - Pros: smallest change, atomic identity allocation, preserves insertion order and existing isolation, uses only JDK primitives.
   - Cons: serializes registration for this in-memory registry.
   - Effort: Low.

2. **Concurrent indexes plus ordered store** — use concurrent maps and atomic `computeIfAbsent` alongside an ordered collection.
   - Pros: allows more concurrent registration throughput.
   - Cons: requires maintaining map/list consistency and deterministic order across structures; solves a scalability need not present in M1.2.
   - Effort: Medium.

## Recommendation

Use the synchronized-registry approach. It makes the existing check/create/append sequence atomic without adding a dependency or a second source of truth. Keep `Work` as the stored identity and routing record with `DeliveryState`; calculate `EffectiveStatus` from the stored delivery state and the one captured query instant. `findByIdempotency` MUST be tenant-scoped and MUST return the stored record even when it is expired; expiry filters delivery, not identity.

One decision is still required for a duplicate `(tenant, idempotencyKey)` whose other fields differ (gateway, correlation, or expiry): the current implementation silently returns the first record, but the requested hardening does not define whether to retain that behavior or reject the conflicting request.

## Risks

- Changing status types can break package-local callers/tests if stored `Status` is removed without migrating them to `EffectiveStatus`.
- A map-based concurrency redesign would risk losing registration order or splitting identity between indexes; avoid it unless throughput becomes a measured need.
- Duplicate-key requests with conflicting payload remain undefined until the proposal chooses reuse or rejection semantics.

## Ready for Proposal

Yes — create an independent successor proposal limited to H1–H4. Carry forward tenant/gateway isolation, deterministic registration order, pre-poll registration, and the validation-only trust boundary without Work creation or M1.1 changes. Exclude M1.3+, HTTP wiring, long polling, persistence, HTTP/API delivery, leases, ACKs, redelivery, reconnect behavior, results, brokers, and any native-attempt/predecessor-state change. The proposal must resolve the one conflicting-duplicate-request decision above.

## Result Contract

This exploration creates only this successor artifact. It makes no source or test change, runs no test, performs no commit/push/PR action, and preserves predecessor `m1-2-remote-work-registration` as BLOCKED/maintainer_decision historical state with native attempt 15 untouched.
