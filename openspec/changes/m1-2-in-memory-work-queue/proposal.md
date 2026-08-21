# Proposal: M1.2 In-Memory Work Queue

## Intent

Add the smallest framework-neutral queue that makes M1.1-authorized synthetic work available to its addressed gateway without transport or recovery semantics.

## Scope

### In Scope
- In-memory pending, delivery, and tenant-scoped correlation-status indexes.
- Target-gateway-only selection; deterministic order among eligible, unexpired work.
- Absolute expiry using an injected `Clock`; expired work is never delivered or revived.
- Reuse M1.1 contracts. `SyntheticTrustBoundary` remains the sole Work and `(tenant, idempotencyKey)` authority; the queue consumes its returned Work.

### Out of Scope
- HTTP, long polling (M1.3), WebSocket, lease, ACK, redelivery, reconnect, retry, gateway results, `COMPLETE`/`PARTIAL`, real retrieval, or production security.
- SQL, Redis, brokers, dependencies, and canonical outcome/trace changes.

## Capabilities

### New Capabilities
- `in-memory-work-queue`: framework-neutral, tenant-isolated pending-work delivery and status lookup.

### Modified Capabilities
- None.

## Approach

Index the Work accepted by `SyntheticTrustBoundary` as pending for one target gateway. Querying delivery filters tenant, target, expiry, then applies a specified stable ordering. Status lookup requires the owning tenant and correlation ID. No queue-side logical-operation creation or idempotency map is added.

## Legacy Artifact Reconciliation

- `m1-synthetic-remote-gateway-search/proposal.md`: rename M1.2 to the queue, move long-poll/process proof to M1.3, and move aggregation to M1.4.
- `design.md`, `tasks.md`, and `specs/synthetic-remote-gateway-search/spec.md`: make the same M1.2/M1.3/M1.4 allocation; preserve M1.1 and leave outcome/trace artifacts untouched.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `synthetic-retrieval/.../InMemoryWorkQueue.java` | New | In-memory queue indexes. |
| `synthetic-retrieval/.../InMemoryWorkQueueTest.java` | New | Controlled-clock queue behavior. |
| `openspec/changes/m1-synthetic-remote-gateway-search/` | Modified | Minimal milestone-label reconciliation. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Duplicate identity ownership | Medium | Boundary remains the sole logical-operation authority. |
| Nondeterministic map iteration | Medium | Specify an explicit eligible-work ordering. |

## Rollback Plan

Revert the queue-only delivery unit and its indexes; M1.1 authority remains intact and no persistent state exists.

## Dependencies

- M1.1 `SyntheticTrustBoundary`, opaque types, and injectable `Clock`.

## Success Criteria

- [ ] Only target-gateway, unexpired work is delivered in deterministic order.
- [ ] Tenant-scoped idempotency and correlation-status isolation hold without duplicate Work creation.
- [ ] No transport, recovery, result, dependency, or canonical-spec behavior is introduced.
