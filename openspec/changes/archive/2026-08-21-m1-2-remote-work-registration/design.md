# Design: M1.2 Remote Work Registration

Registration is the sole source of remote-work identity. `InMemoryWorkRegistry` creates or reuses a stored `PENDING` record before polling; `SyntheticTrustBoundary` only validates a poll, and a lookup reads an already registered operation after that validation.

## Technical Approach

Add one framework-neutral, package-local in-memory registry in `synthetic-retrieval`. It receives an injected `Clock`, assigns deterministic operation IDs in registration order, and indexes the same immutable record for tenant/idempotency, tenant/correlation, and tenant/gateway eligible-work reads. The existing boundary keeps its binding, time, authorization, and nonce checks, but no longer owns operation state or returns a `Work`.

## Architecture Decisions

| Decision | Alternatives considered | Rationale |
|---|---|---|
| Registry owns registration and idempotency | Keep `operations` in `SyntheticTrustBoundary` | A poll must not create remote work; one owner prevents split identity authority. |
| Preserve one immutable stored `Work` record | Separate DTOs or reconstructed poll results | Retains tenant, target gateway, operation ID, idempotency key, correlation ID, expiry, delivery state, and status exactly and prevents divergent views. |
| Boundary returns validation-only `Decision` with validated tenant/gateway identifiers | Return `Accepted(Work, Trace)` | Lookup needs only validated routing identifiers; returning work reintroduces authorization-side creation coupling. |
| Use insertion-ordered storage plus filtered iteration | Sort dynamically or use unordered maps | Registration order is the required deterministic order and needs no extra policy. |

## Data Flow

    Registration request
           │
           ▼
    InMemoryWorkRegistry.register(request, Clock)
           │ creates/reuses PENDING immutable record
           ▼
    tenant/idempotency + tenant/correlation + ordered record store

    Poll ──► SyntheticTrustBoundary.authorize(poll)
                       │ validation-only accepted identifiers
                       ▼
              registry.eligible(tenant, gateway)
                       │ PENDING and expiresAt > clock.instant()
                       ▼
                 preregistered ordered Work records

## File Changes

| File | Action | Description |
|---|---|---|
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` | Create | Package-local registration, tenant-scoped indexes, expiry filtering, and status lookup. |
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/SyntheticTrustBoundary.java` | Modify | Remove operation map and work creation; expose only validated poll identifiers in acceptance. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` | Create later | RED/Green coverage; **only RED-1 is authorized first**. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/SyntheticTrustBoundaryTest.java` | Modify later | Replace M1.1 authorization-side work assumptions with validation-only regression coverage. |

## Interfaces / Contracts

```java
final class InMemoryWorkRegistry {
    InMemoryWorkRegistry(Clock clock);
    Work register(Registration request);              // tenant/idempotency reuse
    List<Work> eligible(TenantId tenant, GatewayId gateway);
    Optional<Status> status(TenantId tenant, CorrelationId correlation);
}

record Registration(TenantId tenant, GatewayId targetGateway,
                    IdempotencyKey idempotencyKey, CorrelationId correlationId,
                    Instant expiresAt) { }
record Work(TenantId tenant, GatewayId targetGateway, OperationId operationId,
            IdempotencyKey idempotencyKey, CorrelationId correlationId,
            Instant expiresAt, DeliveryState deliveryState, Status status) { }
```

`eligible` returns only the requesting tenant's target-gateway records that remain `PENDING` and unexpired (`expiresAt > clock.instant()`); expired records never revive. `status` returns empty for unknown or cross-tenant correlation IDs. All identifier records retain existing opaque-ID validation. The final types will reuse the boundary's identifier records where possible, without adding an adapter or transport abstraction.

## Testing Strategy

| Layer | What to test | Approach |
|---|---|---|
| Unit — RED-1 only | `registersPendingWorkBeforeAnyGatewayPoll` | First create `InMemoryWorkRegistryTest`, run `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#registersPendingWorkBeforeAnyGatewayPoll test`, preserve full output and required strict-RED metadata, then stop. |
| Unit — RED-2..6 | Idempotency/isolation, routing, expiry, order, correlation, boundary regression | Explicitly deferred until RED-1 evidence is complete and separately authorized. |
| Integration/E2E | None | Out of scope: no transport, persistence, leases, ACKs, redelivery, results, or M1.3+ behavior. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. This replaces the invalid authorization-side operation ownership in one in-memory module; no compatibility path restores it.

## Open Questions

None.

## Result Contract

This design defines the implementation plan only. It authorizes neither source/test changes nor command execution. RED-1 must be created and run with preserved full output before any GREEN work; RED-2 through RED-6 remain deferred.
