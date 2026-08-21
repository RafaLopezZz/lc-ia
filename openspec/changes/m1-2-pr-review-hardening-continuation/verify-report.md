```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:34292114d91bf331ec35d8f20af1f9d678618d2e560bfc532d3324a74ff634b3
verdict: fail
blockers: 1
critical_findings: 1
requirements: 2/2
scenarios: 6/6
test_command: mvn -pl synthetic-retrieval "-Dtest=InMemoryWorkRegistryTest#pendingQueryUsesOneSnapshotAcrossExpiryBoundary" test
test_exit_code: 0
test_output_hash: sha256:34292114d91bf331ec35d8f20af1f9d678618d2e560bfc532d3324a74ff634b3
build_command: not run; prohibited by ordinal-5 scope
build_exit_code: 0
build_output_hash: sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

## Verification history

- H3-R (ordinal 1): GREEN_BASELINE, REFACTOR, GREEN_REGRESSION. RED: not required.
- H4 (ordinal 2): RED recorded: pending query returned three work items and `Clock.instant()` was called three times.
- H4 (ordinal 3): GREEN recorded: pending query returned three work items and `Clock.instant()` was called once.
- H4 boundary characterization (ordinal 5): at fixed `T`, candidate A with `expiresAt=T` was excluded, candidate B with `expiresAt>T` was included, and one reset `CountingClock` recorded one `instant()` call for one `pendingFor` query.

## Historical exception

- BLOCKED: predecessor H3 native-settlement exception remains historical only. It is not PASS evidence for this continuation and is neither inherited nor reset.

## Verdict

FAIL — the ordinal-5 focused characterization passed, but the preserved historical native-settlement exception remains blocked. No suite, reactor, production change, Git/PR action, or settlement was performed.
