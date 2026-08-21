# Apply Progress: M1.2 PR Review Hardening

## Task progress

- [x] H1 / 1.1 — canonical Green evidence preserved from the prior assertion-order correction.
- [x] H2 / 2.1 — `expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable` added and run once.
- [ ] H3 / 3.1 — technical GREEN PASS is preserved, but native settlement is **BLOCKED**; it is not settled PASS.
- [ ] H4 / 4.1 — not started.

## H2 canonical RED evidence

- Authorization binding: `sha256:6f733b171216d90bca034e1b830d374a8579e0f1194b79e5ebc6b9192d14e806`.
- Test-only change: `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` adds only `expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable`.
- Command, run exactly once: `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable test`.
- UTC start: `2026-08-20T11:55:19.2862778Z`; UTC end: `2026-08-20T11:55:21.7178630Z`; exit code: `1`.
- Maven result: tests run `1`; failures `0`; errors `1`; skipped `0`; build failure.
- Causality: `InMemoryWorkRegistry` has no `findByIdempotency(TenantId, IdempotencyKey)` method. Both tenant-scoped lookup calls in the new test are unresolved compilation problems.
- Classification: **TRUE RED — missing required API/behavior**. It is not characterization.

## H2 acceptance assertions

The test uses `MutableClock` with an already-expired tenant-a/key-a work and explicitly captures `Optional<Work>` from `findByIdempotency(tenant-a, key-a)`. It asserts that lookup returns that same work, `pendingFor(tenant-a, gateway-a)` is empty, and `findByIdempotency(tenant-b, key-a)` is empty. It does not use the `pendingFor(tenant, key)` overload.

## Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkRegistryTest#expiredWorkRemainsFindableByIdempotencyButIsNotDeliverable test`; exit `1`; tests `1`; failures `0`; errors `1`; skipped `0`. |
| Runtime harness command/scenario and exact result | N/A — an in-memory JUnit registry test has no external runtime boundary. |
| Rollback boundary | Revert only the H2 test, its task checkbox, and this evidence file; production source remains untouched. |

## Stop point

Stopped after the one permitted focused Maven execution. No production code, H3, H4, suite, reactor, verification, commit, push, or PR work was performed.

## H3 technical GREEN and maintainer settlement exception

- H3 technical result: **GREEN PASS**. The focused canonical GREEN evidence remains preserved; this record does not reconstruct or replace it.
- Evidence revision: `sha256:9cbafa…`.
- Native attempt: ordinal `8`; id `sha256:fc395d…`.
- Native settlement: **BLOCKED** — `changed_line_budget_exceeded` (maximum `120`; runtime `583`).
- Maintainer exception: no safe reset and no evidence-reuse lineage are demonstrable. Do not source, test, transition native state, or reconstruct evidence.
- Settlement rule: H3 MUST NOT be marked settled PASS. Its technical GREEN PASS is evidence only; task settlement remains pending native authority.

## Verification status

No `verify-report.md` was created or revised. Native settlement is blocked, so no final verification verdict is asserted.
