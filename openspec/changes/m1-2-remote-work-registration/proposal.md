# Proposal: M1.2 Remote Work Registration

## Intent

Supersede the invalidated M1.2 implementation approach, not its valid queue scope. Remote work must be registered before polling so one registry/queue owns operation identity and tenant-scoped idempotency.

## Scope

### In Scope
- In-memory registry/queue creates or reuses PENDING work by tenant and idempotency key before any poll.
- Each record retains tenant, target gateway, operation ID, idempotency key, correlation ID, expiresAt, delivery state, and status.
- Polling validates trust/protocol only and reads eligible work; it MUST NOT create Work.
- Reuse valid queue scope: clock, expiry, isolation, correlation, deterministic ordering, and exclusions.

### Out of Scope
- The invalid authorization-side Work creation and `Accepted`-before-registration flow.
- Persistence, HTTP/API delivery, leases, ACKs, redelivery, reconnect behavior, results, and all M1.3+ behavior.
- Any RED, GREEN, apply-progress, or execution claim in this change phase.

## Capabilities

### New Capabilities
- `remote-work-registration`: pre-poll, tenant-isolated registration and retrieval of synthetic remote work.

### Modified Capabilities
None.

## Approach

Introduce the smallest framework-neutral registry/queue boundary. Registration generates an operation ID and produces PENDING work before polling; duplicate registration in the same tenant reuses the operation. The trust boundary becomes validation-only during poll.

The first executable task is **RED-1 only**: add `registersPendingWorkBeforeAnyGatewayPoll`, run its focused command while the production registration API is absent, and preserve exact command/output, timestamps, snapshots, exit code, output hash, and ordering proof before any GREEN work. Do not create apply-progress or claim RED until that evidence exists.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `synthetic-retrieval/.../SyntheticTrustBoundary.java` | Modified | Remove Work-creation side effect from polling authorization. |
| `synthetic-retrieval/.../SyntheticTrustBoundaryTest.java` | Modified | Replace superseded authorization-side creation contract. |
| `synthetic-retrieval` registry/queue boundary | New | Register PENDING work and expose eligible ordered work. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Boundary move breaks poll semantics | Medium | Specify validation-only polling and test registration before poll. |
| Cross-tenant reuse or lookup | Medium | Key idempotency and correlation access by tenant. |

## Rollback Plan

Revert the successor implementation as one unit; do not restore the invalidated authorization-side creation approach. Keep the historical invalidation record unchanged.

## Dependencies

- Accepted delta specification for `remote-work-registration`.
- Preserved RED-1 evidence before production implementation.

## Success Criteria

- [ ] Registration creates or reuses tenant-scoped PENDING work before every poll.
- [ ] Poll trust/protocol validation has no Work-creation side effect.
- [ ] Queue records retain all required identity, routing, expiry, delivery, and status fields.
- [ ] RED-1 evidence precedes all GREEN work; M1.3+ behavior remains excluded.
