# Exploration: M1.2 PR Review Hardening Continuation

## Current State

LCIA-23 is on `feat/m1-2-in-memory-work-queue` for PR #5. The accepted baseline is H1/H2 complete. H3 is a technical PASS with its native exception already documented; this continuation does not re-run H1–H3 or alter predecessor/native state.

`InMemoryWorkRegistry` already stores `DeliveryState` and exposes `EffectiveStatus`, but it also retains `Status`, `statusFor`, and `Work.status`. Those parallel status representations permit stale or contradictory reads and are the H3-R removal/migration scope. `pendingFor(TenantId, GatewayId)` currently calls `clock.instant()` within the stream predicate, so a single gateway query can evaluate different times; H4 requires one snapshot at query start.

## Affected Areas

- `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` — remove `Status`, `statusFor`, and `Work.status`; make `EffectiveStatus` the derived read model while retaining `DeliveryState`; capture one query-time instant before filtering pending work.
- `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` — migrate status assertions to `EffectiveStatus` and characterize a single clock snapshot for a pending gateway query.
- `openspec/changes/m1-2-pr-review-hardening-continuation/exploration.md` — independent continuation record only.

## Approaches

1. **Remove the legacy status surface** — delete `Status`, `statusFor`, and the `Work.status` record component; retain `DeliveryState`; expose derived `EffectiveStatus` through the existing effective-status query/helper and migrate package-local callers.
   - Pros: one status vocabulary, no stored/derived divergence, smallest focused migration.
   - Cons: package-local callers must change together.
   - Effort: Low.

2. **Keep legacy status as a deprecated adapter** — retain `Status` and map it from `EffectiveStatus` temporarily.
   - Pros: fewer immediate caller edits.
   - Cons: preserves the duplicate vocabulary H3-R explicitly removes and extends migration risk.
   - Effort: Low.

## Recommendation

Use approach 1. `DeliveryState` remains the stored lifecycle state; `EffectiveStatus` is calculated from it and an explicit clock instant. In `pendingFor`, capture `Instant now = clock.instant()` before the stream and evaluate every candidate against `now`. Do not acquire work in this continuation: record only a planned future acquire budget of 750 changed lines.

## Risks

- Removing `Work.status` changes the record constructor and package-local test/API shape; all current usages must migrate atomically.
- The effective-status derivation must explicitly retain `DeliveryState` semantics as lifecycle states expand beyond `PENDING`.
- A single snapshot must be captured once per gateway query, not once per candidate or after candidate selection.

## Ready for Proposal

Yes — propose the independent H3-R/H4 continuation only. Preserve H1/H2 as accepted and H3 technical PASS with its documented native exception; do not re-run H1–H3. Exclude acquire execution, source/test changes during exploration, predecessor/native-state changes, and all M1.3+ delivery work.

## Result Contract

This exploration creates only this independent continuation artifact in both stores. It performs no source or test modification, no command, no acquire, and no commit, push, or PR action. The planned future acquire budget is 750 changed lines only.
