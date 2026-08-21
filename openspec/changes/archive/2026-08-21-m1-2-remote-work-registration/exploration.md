# Exploration: M1.2 Remote Work Registration

## Current State

`SyntheticTrustBoundary.authorize(Poll)` currently validates a gateway poll and uses `(tenant, idempotencyKey)` to create or reuse `Work`. The HTTP adapter invokes it only from `/synthetic/poll`. There is no registry that can register remote work before a gateway polls.

The prior queue attempt is historical only: it has no recoverable per-scenario RED chronology and its `record(Accepted)` input makes service-first registration impossible.

## Affected Areas

- `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/SyntheticTrustBoundary.java` — currently owns Work creation and idempotency during authorization; this responsibility must move out.
- `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/SyntheticTrustBoundaryTest.java` — documents the current authorization-side Work/idempotency behavior that the successor supersedes.
- `lc-ia-server/src/main/java/com/leovinci/leos/adapters/in/https/SyntheticRemoteGatewayHttpBoundary.java` — calls `authorize` only within the poll endpoint; registration must precede this read path.
- `openspec/changes/m1-2-in-memory-work-queue/verify-report.md` — historical invalidation note preserves the rejected attempt without altering its claimed evidence.

## Scope Reuse and Supersession

| Prior scope | Successor treatment |
|---|---|
| Queue scope, absolute expiry, tenant isolation, correlation lookup, deterministic ordering, and exclusions | Reuse. |
| Boundary-owned Work creation and idempotency | Supersede: the registry/queue owns operation identity and idempotency. |
| `Accepted` before queue registration | Supersede: registration creates PENDING work before any poll; the boundary only validates trust/protocol. |

## Approaches

1. **Registry-first in-memory registration** — register or reuse a PENDING operation before polling; polling validates and reads eligible pending work.
   - Pros: supports the required lifecycle and gives one owner for identity/idempotency.
   - Cons: changes the M1.1 boundary contract.
   - Effort: Medium.

2. **Retain `Accepted`-driven queue registration** — keep Work creation inside `authorize` and index it after polling.
   - Pros: smaller local change.
   - Cons: cannot register service work before a poll; rejected.
   - Effort: Low.

## Recommendation

Use registry-first in-memory registration. The registry/queue MUST create or reuse one PENDING work record keyed by tenant and idempotency key before any gateway poll. Each record holds tenant, target gateway, operation ID, idempotency key, correlation ID, expiresAt, status, and delivery state. The trust boundary MUST validate trust/protocol only and MUST NOT create remote work as an authorization side effect.

## First RED-only Plan

**Scenario**: `registersPendingWorkBeforeAnyGatewayPoll`

1. Add one focused unit test for the smallest public registration boundary using a fixed `Clock`.
2. Register a synthetic request containing tenant, target gateway, idempotency key, correlation ID, and absolute expiry without constructing or invoking a poll/boundary authorization.
3. Assert the returned/observed record is PENDING, preserves the supplied tenant, gateway, idempotency key, correlation ID, and expiry, and has a generated operation ID.
4. Run only that test while the registration production API is absent; it MUST fail for the missing registration boundary, not for unrelated setup.
5. Capture the RED evidence before writing production code. Do not claim GREEN, apply progress, or completion.

### Required Chronological Evidence Fields

| Field | Required value |
|---|---|
| Scenario and test identifier | `registersPendingWorkBeforeAnyGatewayPoll` and its exact test path/method |
| Written timestamp and source snapshot | UTC timestamp plus immutable diff/blob hash before execution |
| Pre-RED production snapshot | Git/worktree identity and statement that registration production API is absent |
| Exact command | Focused Maven/JUnit command only |
| RED start/end timestamps | UTC timestamps surrounding the command |
| Exit code and output hash | Exact non-zero exit code and full-output SHA-256/reference |
| Failure assertion | Missing registration boundary/API is the direct failure cause |
| Ordering proof | Evidence was captured before any production implementation change |

## Result Contract

- This exploration creates no apply-progress and makes no RED, GREEN, or execution claim for the successor.
- The successor remains in-memory and framework-neutral.
- Out of scope: persistence, HTTP, lease, ACK, redelivery, and M1.3+ behavior.

## Risks

- Moving Work creation out of the trust boundary requires an explicit successor contract for poll validation over pre-registered work.
- Idempotency reuse must preserve tenant isolation and must not let correlation lookup cross tenants.

## Ready for Proposal

Yes — propose the registry-first contract, explicitly superseding authorization-side Work creation and `Accepted`-before-registration flow.
