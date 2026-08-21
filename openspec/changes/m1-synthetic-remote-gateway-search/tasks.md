# Tasks: M1 Synthetic Remote-Gateway Search

Synthetic-only proof; no production-security, identity, crypto, persistence, provider, or dependency claim. No size exception is required.

## Review Workload Forecast

| Field | Value |
|---|---|
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |
| Review budget | 800 changed lines per delivery/review PR |
| Forecast | M1.1 680–760; M1.2 280–360; M1.3 520–680; M1.4 300–400 |
| Split | Four self-contained PRs; only the tracker targets `main` |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

Tracker `feature/m1-synthetic-remote-gateway-search` is draft/no-merge. A child diff must show only its slice; retarget/rebase any polluted diff.

## Delivery Units

| PR | Dependency / target | Executable behavior | Gates | Clean diff and rollback |
|---|---|---|---|---|
| M1.1, 680–760 | `feature/m1.1-trust-boundary` → tracker | Actual HTTP edge enforces all M1.1 binding, revocation, `Clock` expiry, replay, idempotency, remote/local authorization, minimized-data, and operation/attempt invariants. | Focused: `mvn -B -f lc-ia-server/pom.xml -Dtest=SyntheticRemoteGatewayHttpBoundaryTest test`; regression: core `jdeps`; server: `mvn -B clean verify`. | Reactor, core ledger/contracts, HTTP edge, tests; revert M1.1 only. |
| M1.2, 280–360 | `feature/m1.2-in-memory-work-queue` → M1.1 | Target-gateway-only in-memory delivery, tenant-scoped idempotency, absolute expiry, correlation status, and deterministic eligible ordering; no lease/ACK/result behavior. | Focused: `mvn -B -f synthetic-retrieval/pom.xml -Dtest=InMemoryWorkQueueTest test`; regression: core suite + `jdeps`; no server change. | Queue indexes and tests; revert M1.2 only. |
| M1.3, 520–680 | `feature/m1.3-process-proof` → M1.2 | Two child JVMs prove outbound JDK `HttpClient` HTTPS long poll, distinct PIDs, no gateway listener/object shortcut, minimized round trip. | Focused: `mvn -B -f lc-ia-server/pom.xml -Dtest=RemoteGatewayProcessE2ETest test`; regression: `ArchitectureBoundaryGuardsTest`, core `jdeps`, `mvn -B clean verify`. | Process mains, JDK transport/TLS harness, E2E; revert M1.3 only. |
| M1.4, 300–400 | `feature/m1.4-frozen-aggregation` → M1.3 | Frozen coverage, permutation-stable sorted aggregate, `COMPLETE` or conservative `PARTIAL`. | Focused: core scenario command above; regression: core suite + `jdeps`; no server change. | Coverage/aggregator and tests; revert M1.4 only. |

## M1.1 — Trust boundary at the actual HTTP edge
- [x] Add RED `lc-ia-server/src/test/java/.../SyntheticRemoteGatewayHttpBoundaryTest.java` cases for every M1.1 accept/reject invariant, prohibited DTO/trace fields, and prevailing local denial.
- [x] Add `synthetic-retrieval/src/{main,test}/java/...` records, credential registry, `Clock` ledger, and JUnit `jdeps` guard banning Spring, JPA/Hibernate, server, and adapter references.
- [x] Add `lc-ia-server/src/main/java/.../adapters/in/https` allowed-field conversion and both authorization checks; run M1.1 gates.

## M1.2 — In-memory work queue
- [ ] Add RED `InMemoryWorkQueueTest` cases for target delivery, tenant-scoped idempotency, controlled-clock expiry, correlation status, and deterministic eligible ordering.
- [ ] Implement `synthetic-retrieval/src/main/java/...` pending/delivery/status indexes consuming boundary-created Work; run M1.2 gates.

## M1.3 — Independent-process HTTPS proof
- [ ] Add RED `lc-ia-server/src/test/java/.../RemoteGatewayProcessE2ETest.java` for all M1.3 process/transport assertions.
- [ ] Add JDK `HttpsServer`/`HttpClient` mains and argument-list `ProcessBuilder` wiring in `lc-ia-server/src/main/java/...`; cleanup children/TLS in `finally`; run M1.3 gates and confirm its diff is clean against M1.2.

## M1.4 — Frozen deterministic aggregation
- [ ] Add RED core cases for mutation-after-freeze, permutations, `COMPLETE`, and missing/denied/expired/incompatible `PARTIAL`.
- [ ] Implement immutable coverage and sorted aggregation in `synthetic-retrieval/src/main/java/...`; run M1.4 gates.
