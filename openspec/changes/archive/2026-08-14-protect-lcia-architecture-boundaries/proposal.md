# Proposal: Protect LC-IA Architecture Boundaries

## Intent

Establish executable M0.4 guardrails in the real `lc-ia-server` module before its domain and adapter surface grows. The guards prevent infrastructure coupling from becoming implicit architecture while proving that every rule can detect a violation.

## Scope

### In Scope
- Add architecture guards that analyze dependencies of compiled `lc-ia-server` classes and execute through `mvn test`.
- Enforce that domain code does not depend on Spring, JPA/Hibernate, or adapters.
- Enforce that controllers do not directly depend on concrete persistence adapters.
- Add minimal, isolated controlled-violation fixtures under `lc-ia-server/src/test/java` and verify each identical rule rejects its fixture.

### Out of Scope
- Changes to `synthetic-retrieval`, which remains an independent laboratory.
- Fake production packages, placeholder production classes, or an imposed package hierarchy.
- Fixed architecture-library versions, concrete package predicates, and Maven bans as the primary enforcement mechanism.
- M0.5 CI workflow creation or modification. M0.4 integrates the guards into normal `mvn test` and leaves them ready for CI.
- Runtime behavior, persistence implementation, controllers, or domain features.

## Capabilities

### New Capabilities
- `architecture-boundary-guards`: Executable compiled-class dependency rules with controlled test-only violation proof.

### Modified Capabilities
- None.

## Approach

Add a test-scoped dependency managed by the existing build where possible. Evaluate each rule against dependencies of real compiled `src/main/java` classes, then evaluate the same rule against its dedicated compiled `src/test/java` violating fixture and assert rejection. Sparse production rules may pass vacuously when affected classes do not yet exist; controlled fixtures independently prove that the guard detects the violation when it exists. Keep fixtures separate from production imports. Maven dependency bans may later complement a real module boundary, but cannot replace class-dependency guards.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `lc-ia-server/pom.xml` | Modified | Test-only guard dependency and normal test execution. |
| `lc-ia-server/src/test/java/` | New | Rules and controlled violating fixtures. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Fixtures affect production checks | Low | Import production and fixtures separately per evaluation. |
| Premature topology hardening | Medium | Defer package predicates and hierarchy to design. |

## Rollback Plan

Revert the test dependency and architecture-guard test sources only; no production behavior or data changes are introduced.

## Dependencies

- Existing `lc-ia-server` Maven test lifecycle and dependency management.

## Success Criteria

- [ ] `mvn test` executes and passes the production architecture guards.
- [ ] Each guard demonstrably rejects its corresponding test-only violation.
- [ ] M0.4 adds no CI workflow; the Maven-integrated guards remain ready for M0.5 CI wiring.
- [ ] No production fake classes or `synthetic-retrieval` changes are introduced.
- [ ] Delivery remains one work-unit commit and within the 800-line single-PR budget.
