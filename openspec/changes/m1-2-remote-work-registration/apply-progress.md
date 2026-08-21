# Apply Progress: M1.2 Remote Work Registration

## Cumulative State

- RED-1 through GREEN-5 are complete.
- GREEN-5 was authorized by `sha256:32c59ca37b7f4efd05792364cc13d65ec1a6e255303e9b2e23d3ceeec25ff44b`.
- RED-6 was evaluated with `sha256:64ac97623a420a77b9e1198cd0d9f3ab9b5aa9ecf37f648ee5a86b1af5507c2a` and is a passing characterization/regression, not a RED.
- GREEN-2.6 completes the M1.1 validation-only boundary regression after direct passing regressions.

## RED-6 Characterization Evaluation

### Test written

`InMemoryWorkRegistryTest.deliversEligibleWorkInDeterministicOrderWithoutCrossTenantLeakage` registers three unexpired records for the same tenant and gateway, interleaves one record for another tenant on that gateway, and asserts that the eligible result is exactly the three owning-tenant records in registration order.

### Focused command and result

```text
mvn -B -pl synthetic-retrieval "-Dtest=InMemoryWorkRegistryTest#deliversEligibleWorkInDeterministicOrderWithoutCrossTenantLeakage" test
```

Executed exactly once at `2026-08-19T17:18:01+02:00`; exit `0`.

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  1.467 s
```

### Classification and causality

This is a characterization/regression result, not RED evidence: the existing `pendingFor(TenantId, GatewayId)` filters by tenant and gateway while preserving insertion order from the existing `works` list and sequential stream. No production source was modified, so there is no absence to demonstrate and no Green work is authorized by this result.

## Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | Exact command above, executed once; exit `0`; 1 test, 0 failures, 0 errors, 0 skipped. |
| Runtime harness | N/A — framework-neutral in-memory unit behavior only. |
| Rollback boundary | Revert only `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` and this evidence file. |

## Historical Stop Condition

No production change, Green work, task completion mark, commit, push, PR, priority, consumption, state transition, lease, ACK, retry, concurrency, HTTP, persistence, M1.3+ work, or boundary change.

## GREEN-2.6: Validation-only Trust Boundary

### Contract and types

`SyntheticTrustBoundary.authorize(Poll)` now authorizes trust/protocol only. `Accepted` carries only a minimized `Trace(tenantId, gatewayId, correlationId, AUTHORIZED)`; it carries no `Work`.

- Removed boundary-local `operations`, `Key`, `computeIfAbsent`, `Work`, `AttemptId`, and boundary `CandidateId`.
- Retained `OperationId` because `InMemoryWorkRegistry` remains the sole registration/work authority and generates it for pre-registered records.
- Preserved M1.1 binding, revocation, fixed-Clock expiry, remote authorization, nonce replay, opaque-identifier validation, trace minimization, and HTTP local-final authorization guarantees.

### HTTP result

The HTTP boundary preserves request parsing, remote trust validation, and local authorization. Its successful response is exactly `trace=AUTHORIZED`; it has no registry dependency, registration call, work creation, or long-poll behavior.

### RED evidence

Native authorization token: `sha256:9f57b63eb8d5d5905d56aaee0095d4acf39211ae8cdba62e0400f53b2b08bff3`.

The supplied focal test initially failed on `2026-08-19T18:14:29+02:00` with exit `1`: `m11AuthorizationCreatesNoRemoteWork` received an authorization-created `Work[operationId=operation-1, attemptId=attempt-1, ...]` instead of no remote work.

### Direct regression evidence

| Command | Result |
|---|---|
| `mvn -B -pl synthetic-retrieval "-Dtest=SyntheticTrustBoundaryTest#m11AuthorizationCreatesNoRemoteWork" test` | PASS after a clean compilation; 1 test, 0 failures, 0 errors, 0 skipped; exit `0`. |
| `mvn -B -pl synthetic-retrieval "-Dtest=SyntheticTrustBoundaryTest" test` | PASS; 6 tests, 0 failures, 0 errors, 0 skipped; exit `0`. |
| `mvn -B -f lc-ia-server/pom.xml "-Dtest=SyntheticRemoteGatewayHttpBoundaryTest" test` | PASS after a clean compilation; 3 tests, 0 failures, 0 errors, 0 skipped; exit `0`. |

No full suite, reactor build, SDD verify, commit, push, or PR was run.

### Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | The focal M1.1 regression command passed after the production refinement: 1 test, 0 failures/errors/skips, exit `0`. |
| Runtime harness | The direct HTTP boundary test passed over the local HTTP adapter: 3 tests, 0 failures/errors/skips, exit `0`. |
| Rollback boundary | Revert the refined boundary and HTTP response/test files plus this task/evidence update; retain `InMemoryWorkRegistry` as the independent operation/work authority. |

## Result Contract

GREEN-2.6 is complete only for the validation-only boundary refinement and its direct passing regressions. Registration, registry ownership, parsing/trust/local authorization, and all M1.3+ behavior remain otherwise unchanged.

## Task Completion

- [x] 2.6 GREEN-2.6 is complete from the preserved architectural RED, authorized GREEN-2.6, M1.1 focal regression 1/1, `SyntheticTrustBoundaryTest` 6/6, and HTTP boundary 3/3 evidence above.
- Final task checkbox count: 12/12 complete.
