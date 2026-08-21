# Design: M1.2 In-Memory Work Queue

Implement one framework-neutral in-memory index in `synthetic-retrieval`. It records `SyntheticTrustBoundary.Accepted` values and exposes eligible pending Work plus tenant-scoped correlation status. M1.1 remains unchanged: the trust boundary alone creates/reuses Work for `(tenant, idempotencyKey)`.

## Technical Approach

`InMemoryWorkQueue` receives an injected `Clock`; it never calls the system clock. `record(accepted)` derives tenant and target gateway from the accepted trace, retains the returned Work unchanged, and assigns a monotonically increasing acceptance sequence only for a newly recorded `(tenant, operationId)`.

Queries evaluate `work.expiresAt()` against `clock.instant()` on every call. They filter by tenant, target gateway, and `expiresAt().isAfter(now)`, then return ascending acceptance sequence. Expired records remain inert indexes: no activity extends, removes, revives, leases, or redelivers them.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|---|---|---|---|
| Identity authority | Queue consumes `Accepted`; deduplicates its record by `(tenant, operationId)` only | Queue-side `(tenant, idempotencyKey)` map or Work construction | The boundary owns logical-operation identity and idempotency; the queue is only an index of accepted Work. |
| Routing source | Derive tenant/gateway from `Accepted.trace()` | Add mutable route fields to `Work` | Work and trace are existing M1.1 contracts; this adds no identity or transport contract. |
| Time and expiry | Inject `Clock`; absolute `expiresAt > now` eligibility | `Instant.now()`, TTL refresh, cleanup timer | Deterministic tests and no expiry revival. |
| Ordering | Store a `long acceptanceSequence`; sort/filter by it | Map iteration or timestamps | FIFO is explicit and deterministic, including equal clock instants. |

## Data Flow

    SyntheticTrustBoundary.authorize(Poll)
                  │ Accepted(Work, Trace)
                  ▼
       InMemoryWorkQueue.record(accepted)
          ├─ operation record index
          ├─ tenant + target pending index
          └─ tenant + correlation index
                  │
       pendingFor(tenant, gateway) / status(tenant, correlation)

`record` is idempotent for the same boundary Work record and does not create a second logical operation. `pendingFor` is a read, not delivery acknowledgement; therefore repeated calls return the same eligible Work in the same order.

## File Changes

| File | Action | Description |
|---|---|---|
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkQueue.java` | Create | Framework-neutral in-memory accepted-Work indexes and queries. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkQueueTest.java` | Create | Controlled-clock RED-first queue contract tests. |
| `pom.xml`, `synthetic-retrieval/pom.xml`, `lc-ia-server/**` | No change | No POM, dependency, server, HTTP, polling, or transport change. |

## Interfaces / Contracts

```java
public final class InMemoryWorkQueue {
    public InMemoryWorkQueue(Clock clock);
    public void record(SyntheticTrustBoundary.Accepted accepted);
    public List<SyntheticTrustBoundary.Work> pendingFor(
        SyntheticTrustBoundary.TenantId tenantId,
        SyntheticTrustBoundary.GatewayId gatewayId);
    public Optional<WorkStatus> status(
        SyntheticTrustBoundary.TenantId tenantId,
        SyntheticTrustBoundary.CorrelationId correlationId);
}

public enum WorkStatus { PENDING }
```

Private minimum records: `OperationKey(TenantId, OperationId)`, `CorrelationKey(TenantId, CorrelationId)`, and `QueueRecord(Work, TenantId, GatewayId, long acceptanceSequence)`. Public calls reject null arguments. No queue record contains an idempotency key, retry state, lease, acknowledgement, result, or system-time value.

## Testing Strategy

Implement RED tests before production code, using `Clock.fixed` initially and a small test-only mutable `Clock` only where advancing time is required. Preserve every M1.1 test unchanged.

| Sequence | RED assertion | Minimal GREEN scope |
|---|---|---|
| A | Work appears only for its accepted trace target; another target and another tenant see none. | Target/tenant filtered pending query. |
| B | Two boundary accepts for one tenant/idempotency key yield one returned Work and one queue record; Work identity is unchanged. | Record-by-operation no-op on repeat. |
| C | At exact expiry, and after advancing the injected clock, pending query never returns Work. | Absolute clock comparison. |
| D | Owning tenant gets `PENDING` for correlation; other tenant and unknown correlation get `Optional.empty()`. | Tenant correlation index. |
| E | Interleaved tenant records return only the requested tenant/target Work in ascending acceptance sequence on repeated queries. | Sequence-backed deterministic ordering. |

No integration or E2E test is needed: this is a framework-neutral core with no changed boundary. Existing JUnit/Maven infrastructure is reused; no POM or dependency change is planned.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. The queue is volatile and additive; M1.1 behavior and tests remain unchanged.

## Open Questions

None.
