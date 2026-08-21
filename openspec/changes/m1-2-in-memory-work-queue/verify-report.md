```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:b28828224c2ee328574c975bcee6f3ccaa6dfda673ec013bdb493da30ec7bffd
verdict: fail
blockers: 1
critical_findings: 1
requirements: 6/6
scenarios: 8/8
test_command: mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkQueueTest test
test_exit_code: 0
test_output_hash: sha256:d7b24ea5308be198bbc86e9221d080440b0a9dce59658ba6776a25d7feb7b994
build_command: mvn -B clean verify
build_exit_code: 0
build_output_hash: sha256:40450a0f132e0c7a0344e6e26b1f50ff02c51a9cd7ad2161110f1ae34d744a77
```

## Verification Report

**Change**: m1-2-in-memory-work-queue
**Mode**: Strict TDD (Maven/JUnit)

### Completeness
| Metric | Value |
|---|---:|
| Tasks total | 11 |
| Tasks complete | 11 |
| Tasks incomplete | 0 |
| Changed code/test lines | 231 / 800 |

### Build & Tests Execution
- `mvn -B -pl synthetic-retrieval -Dtest=InMemoryWorkQueueTest test` — PASS; 5/5 tests; exit 0; hash `sha256:d7b24ea5308be198bbc86e9221d080440b0a9dce59658ba6776a25d7feb7b994`.
- `mvn -B -pl synthetic-retrieval -Dtest=SyntheticTrustBoundaryTest#jdepsGuardKeepsTheCoreIndependentOfFrameworkAndServerAdapters test` — PASS; 1/1 test; exit 0; hash `sha256:19a8d691d90bd78461004360c6ed483b8369072ae872e75648ce2e7f1ff9ba1b`.
- `mvn -B -pl synthetic-retrieval test` — PASS; 42/42 tests; exit 0; hash `sha256:28bfa9cdceb996a72bedd430fa3107ddddba930c2ab518a3ac1079551ec78ded`.
- `mvn -B clean verify` — PASS; reactor 42 synthetic-retrieval + 12 lc-ia-server tests; exit 0; hash `sha256:40450a0f132e0c7a0344e6e26b1f50ff02c51a9cd7ad2161110f1ae34d744a77`.
- Coverage and separate quality tooling: not configured.

### Spec Compliance Matrix
| Requirement | Scenarios | Runtime evidence | Result |
|---|---:|---|---|
| Targeted pending-work delivery | 2/2 | Target/tenant assertions in `returnsWorkOnlyToItsAcceptedTenantAndTargetGateway` | ✅ COMPLIANT |
| Boundary-owned logical-operation identity | 2/2 | Identity and one-record assertions in `keepsBoundaryWorkIdentityAndRecordsItsOperationOnlyOnce` | ✅ COMPLIANT |
| Absolute expiry and controlled time | 1/1 | Exact/advanced controlled-clock assertions in `excludesExpiredWorkAndReportsItAsExpiredWithoutRevival` | ✅ COMPLIANT |
| Tenant-scoped correlation status | 1/1 | Owner/other/unknown assertions in `exposesCorrelationStatusOnlyToTheOwningTenant` | ✅ COMPLIANT |
| Deterministic eligible-work ordering | 1/1 | Interleaved/repeated assertions in `returnsEligibleWorkInAcceptanceOrderOnRepeatedTenantGatewayQueries` | ✅ COMPLIANT |
| M1.2 boundary | 1/1 | Source/POM inspection plus passing M1.1 jdeps guard | ✅ COMPLIANT |

### Correctness and Design Coherence
- Queue deduplicates only `(tenant, operationId)` and constructs no Work or idempotency map.
- Routing comes from `Accepted.trace()`; `Clock` is injected; expiry is `expiresAt().isAfter(clock.instant())`.
- Status is explicitly `PENDING` or `EXPIRED`; records are not revived, leased, acknowledged, or redelivered.
- Sequence sorting enforces deterministic FIFO despite HashMap storage.
- Only the two declared Java files are new; POMs, server, and M1.1 boundary are unchanged. No M1.3+ behavior or dependency was added.

### TDD Compliance
| Check | Result | Details |
|---|---|---|
| TDD evidence reported | ✅ | Table exists in apply-progress.md |
| All tasks have tests | ✅ | 6/6 table entries reference the new test file |
| RED confirmed | ❌ | 0/6 RED cells say required `✅ Written`; all report compilation failure instead |
| GREEN confirmed | ✅ | Focused test execution passes 5/5 |
| Triangulation adequate | ✅ | Multiple distinct expected outcomes per behavior |
| Safety net | ✅ | New test file is untracked/new, consistent with N/A |

**TDD Compliance**: 5/6 checks passed.

### Test Layer Distribution
| Layer | Tests | Files | Tools |
|---|---:|---:|---|
| Unit | 5 | 1 | JUnit 5 |
| Integration | 0 | 0 | Not changed |
| E2E | 0 | 0 | Not changed |

### Assertion Quality
**Assertion quality**: ✅ All assertions verify real behavior; empty-list assertions have companion non-empty assertions, and no tautologies, mocks, or ghost loops were found.

### Changed File Coverage
Coverage analysis skipped — no coverage tool detected.

### Issues Found
**CRITICAL**
- Strict TDD requires each RED cell to state `✅ Written`. All six cells report a compilation failure instead, so the mandatory evidence format is incomplete. Runtime behavior passes, but strict-TDD verification cannot pass.

**WARNING**: None.

**SUGGESTION**: None.

### Verdict
FAIL — all 6 requirements and 8 scenarios pass at runtime, but the mandatory Strict TDD RED evidence format fails.

### Historical Invalidation — 2026-08-19

This record is retained as historical evidence only. `m1-2-in-memory-work-queue` MUST NOT be resumed.

- No recoverable chronological, per-scenario RED proof exists: the record contains only a generic missing-symbol claim, not the written test, RED command/output, and pre-GREEN snapshot for each scenario.
- Its architecture requires `InMemoryWorkQueue.record(SyntheticTrustBoundary.Accepted)`, while `SyntheticTrustBoundary.authorize()` creates/reuses `Work` and is invoked from the gateway poll path. Therefore remote service-first registration cannot occur before any gateway poll.

This note does not rewrite prior findings or convert claimed evidence into compliant evidence. A successor starts without apply-progress or RED claims.
