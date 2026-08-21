# Exploration: M1.2 In-Memory Work Queue

## Current State

`feat/m1-2-in-memory-work-queue` and `main` both resolve to `4bd8575`; the working tree was clean during the read-only exploration. No active OpenSpec change named `m1-2-in-memory-work-queue` existed.

The authoritative backlog entry is `docs/jira/LC-IA-jira-backlog-jira-cloud-v4.csv:209-224`. M1.2 is an in-memory remote work queue: work is delivered only to its target gateway, one `(tenant, idempotency key)` identifies one logical operation, expired work is never delivered, status is queryable by correlation ID, and unit tests use a controllable clock. M1.2 must also preserve tenant isolation and deterministic ordering of eligible work.

M1.1 already provides reusable opaque types and an injected `Clock` in `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/SyntheticTrustBoundary.java`. It does not provide queue state, target-gateway delivery selection, correlation-status lookup, or an eligible-work ordering contract.

## Authoritative Scope

### In Scope

- In-memory remote work queue only.
- Delivery only to the addressed gateway.
- One logical operation per tenant-scoped idempotency key.
- Absolute expiry; expired work is never delivered and validity is never extended.
- Status lookup by correlation ID within the owning tenant.
- Tenant isolation, reusable M1.1 contracts/types, injected controllable `Clock`, and deterministic ordering of eligible work.

### Out of Scope

- SQL, Redis, brokers, runtime dependencies, or production-security decisions.
- HTTP, long polling, WebSocket, reconnect, lease, ACK, redelivery, retry, gateway results, and process proof.
- `COMPLETE`/`PARTIAL`, frozen coverage, or real search behavior.

## Affected Areas

- `openspec/changes/m1-2-in-memory-work-queue/` — new change folder; this exploration is its first recoverable artifact.
- `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/SyntheticTrustBoundary.java` — reusable M1.1 types and current duplicate-ownership hazard; change only if moving Work creation to the queue is required by the accepted proposal/design.
- `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkQueue.java` — likely new framework-free queue implementation.
- `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkQueueTest.java` — likely new focused unit tests.
- `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/SyntheticTrustBoundaryTest.java` — change only if M1.1 Work-creation behavior is relocated.

The root POM, module POMs, `lc-ia-server/**`, and `SyntheticRemoteGatewayHttpBoundaryTest.java` are not expected to change for M1.2; HTTP polling is M1.3 work.

## Contradictory Active Artifacts

- `openspec/changes/m1-synthetic-remote-gateway-search/proposal.md:10-13,24,27,31` defines M1.2 as lease/redelivery/ACK/reconnect recovery and includes long polling, aggregation, and process proof.
- `openspec/changes/m1-synthetic-remote-gateway-search/design.md:7,15,19-29,36-42,55-62` assigns leases, results, aggregation, HTTPS/processes, and M1.3 concerns to the umbrella change.
- `openspec/changes/m1-synthetic-remote-gateway-search/tasks.md:12,26-29,36-46` plans recovery under M1.2 and long-poll/process work under M1.4, while the backlog assigns long polling to M1.3.
- `openspec/changes/m1-synthetic-remote-gateway-search/specs/synthetic-remote-gateway-search/spec.md:25-40` specifies at-least-once lease/ACK/result semantics instead of queue acceptance criteria.
- `openspec/changes/m1-synthetic-remote-gateway-search/specs/synthetic-retrieval-outcomes/spec.md` and `openspec/changes/m1-synthetic-remote-gateway-search/specs/minimized-retrieval-trace/spec.md` introduce frozen coverage, `COMPLETE`/`PARTIAL`, and redelivery traces that are outside M1.2.
- `openspec/config.yaml:14-17,22-29` is stale: it reports no implementation, build, or test runner although Maven modules and JUnit tests are present.

## SyntheticTrustBoundary Ownership Warning

`SyntheticTrustBoundary.authorize()` owns `Map<Key, Work>` at line 16 and creates/reuses Work through `operations.computeIfAbsent(new Key(poll.tenantId(), poll.idempotencyKey()), ...)` at lines 53-56. A new queue MUST NOT independently create the same logical operation from the same key.

Further, `SyntheticRemoteGatewayHttpBoundary.poll()` calls `authorize()` at lines 55-63 before evaluating local authorization at lines 68-70. The existing boundary can therefore create Work before returning `LOCAL_DENIED`. The proposal/design must assign one owner for Work registration and idempotency. The recommended owner is the M1.2 queue; the trust boundary remains validation-only if its current creation behavior is relocated.

## Approaches

1. **Queue owns registration and idempotency** — validate through M1.1 contracts, then have the new queue create or reuse logical Work.
   - Pros: one ownership boundary; directly matches the M1.2 backlog; no duplicate `computeIfAbsent` responsibility.
   - Cons: requires a narrow M1.1 behavior adjustment and preservation of existing boundary tests.
   - Effort: Medium.

2. **Trust boundary owns registration and queue consumes returned Work** — retain `authorize()` creation and add only pending/delivery/status state.
   - Pros: smaller immediate source change.
   - Cons: local denial can already create Work; queue ownership remains ambiguous.
   - Effort: Low.

## Recommendation

Create a narrowly scoped M1.2 change and choose queue-owned registration/idempotency. Keep the implementation framework-free and in memory, reuse M1.1 opaque types and injected `Clock`, and specify a deterministic eligibility comparator. Do not change canonical outcome/trace specifications or introduce transport behavior.

## Minimum SDD Reconciliation Plan

1. Add this new change folder's proposal, queue-only delta specification, design, and tasks in subsequent phases.
2. Split or remove only the M1.2/M1.3/M1.4 claims from the active `m1-synthetic-remote-gateway-search` artifacts; preserve M1.1 scope and do not alter archived artifacts.
3. Do not modify canonical `openspec/specs/` outcome/trace specifications for this queue-only change.

## RED Tests

The following tests are proposed, not created or executed:

A. `InMemoryWorkQueueTest.deliversPendingWorkOnlyToItsTargetGateway()`
B. `InMemoryWorkQueueTest.reusesOneLogicalOperationForTenantAndIdempotencyKey()`
C. `InMemoryWorkQueueTest.neverDeliversExpiredWorkWithControlledClock()`
D. `InMemoryWorkQueueTest.returnsStatusByCorrelationIdOnlyWithinTheOwningTenant()`
E. `InMemoryWorkQueueTest.deliversEligibleWorkInDeterministicOrderWithoutCrossTenantLeakage()`

Expected path: `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkQueueTest.java`.
Expected focused command, not run: `mvn -B -f synthetic-retrieval/pom.xml -Dtest=InMemoryWorkQueueTest test`.

## Risks

- Moving Work creation from `SyntheticTrustBoundary` can change M1.1 behavior and needs a compatible, narrowly verified transition.
- Leaving Work creation in the trust boundary permits ambiguous local-denial and queue-registration semantics.
- Hash-map traversal is not a deterministic eligible-work ordering contract.
- The existing active umbrella artifacts must be reconciled without importing M1.3+ decisions into M1.2.

## Ready for Proposal

Yes. The proposal should state the queue-only boundary, tenant-scoped idempotency ownership, absolute-expiry rule, correlation-status lookup, deterministic selection rule, and explicit M1.3+ exclusions.
