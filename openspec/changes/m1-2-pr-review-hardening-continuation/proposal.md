# Proposal: M1.2 PR Review Hardening Continuation

## Intent

Complete the remaining LCIA-23 review hardening on `feat/m1-2-in-memory-work-queue` for PR #5: eliminate parallel status reads and make each pending-work query use one clock snapshot.

**Confirmed predecessor context:** H1/H2 are accepted. H3 has a technical PASS and a documented native-settlement exception. This continuation neither inherits nor resets that exception or any predecessor/native state.

## Scope

### In Scope
- H3-R: establish the existing GREEN baseline, refactor the legacy surface, and prove the required GREEN regression; it is never a RED slice.
- H3-R: remove `Status`, `statusFor`, and `Work.status`; retain `DeliveryState` and use derived `EffectiveStatus`.
- H4: capture one `Instant` at the start of each `pendingFor(TenantId, GatewayId)` query.
- Record only the future acquire limit: at most 750 changed lines.

### Out of Scope
- H1/H2/H3 re-execution, predecessor reset, or native-settlement handling.
- Acquire execution, source changes, test changes, commands, Git actions, or PR updates.
- M1.3+ work and real-data enablement.

## Capabilities

### New Capabilities
- `in-memory-work-registry-continuation`: H3-R status-model consolidation and H4 clock-snapshot consistency for the in-memory registry.

### Modified Capabilities
None; no existing filesystem capability specification is present.

## H3-R Coverage Inventory

| Classification | Coverage | Required result |
|---|---|---|
| GREEN BASELINE | Non-expired and expired `DeliveryState.PENDING` work, correlation lookup, and tenant isolation before refactor. | All listed coverage is GREEN before changing the legacy surface. |
| REFACTOR TARGET | `Status`, `statusFor`, `Work.status`, and their legitimate package-local consumers. | Migrate consumers atomically without changing the classified behavior. |
| GREEN REGRESSION | The full GREEN BASELINE inventory plus absence of the removed surface. | All listed coverage is GREEN after refactor. |
| PRESERVED PREDECESSOR EXCEPTION | H3 technical PASS and documented native-settlement exception. | Historical only; it is neither reused nor reset. |

## Approach

Use the focused removal approach: preserve `DeliveryState` as stored lifecycle state and derive `EffectiveStatus` from it at an explicit instant. Capture `Instant now = clock.instant()` before filtering a gateway's pending work and use it for every candidate.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` | Modified | H3-R legacy-surface removal; H4 query snapshot. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` | Modified | Future assertion migration and snapshot characterization. |
| `openspec/changes/m1-2-pr-review-hardening-continuation/` | New | Independent continuation artifacts. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Record-shape migration breaks package-local callers | Medium | Migrate all current usages in the authorized acquire. |
| Lifecycle semantics collapse into expiry-only logic | Low | Retain `DeliveryState` and derive the effective read model explicitly. |
| Per-candidate clock reads remain | Medium | Snapshot before filtering and characterize one-query behavior. |

## Rollback Plan

Revert only the continuation acquire commit(s); do not reset, alter, or replay predecessor/native settlement records.

## Dependencies

- Accepted H1/H2 baseline and documented H3 technical PASS/native-settlement exception.
- Future acquire authorization limited to 750 changed lines.

## Success Criteria

- [ ] H3-R follows GREEN BASELINE → REFACTOR → GREEN REGRESSION, never RED, and leaves one status vocabulary: stored `DeliveryState` and derived `EffectiveStatus`.
- [ ] H4 evaluates every candidate in one gateway pending query against the same captured instant.
- [ ] Predecessor H3 exception remains documented only, without inheritance or reset.
