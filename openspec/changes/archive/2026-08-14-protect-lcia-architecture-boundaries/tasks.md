# Tasks: Protect LC-IA Architecture Boundaries

## Review Workload Forecast

| Field                        | Value                  |
| ---------------------------- | ---------------------- |
| Estimated changed lines      | 280–380 authored lines |
| 400-line budget risk         | Medium                 |
| 800-line session budget risk | Low                    |
| Chained PRs recommended      | No                     |
| Suggested split              | Single work-unit PR    |
| Delivery strategy            | single-pr              |
| Chain strategy               | pending                |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal                                        | Likely PR | Focused test command                             | Runtime harness                   | Rollback boundary                             |
| ---- | ------------------------------------------- | --------- | ------------------------------------------------ | --------------------------------- | --------------------------------------------- |
| 1    | Enforce and prove compiled-class boundaries | Single PR | `mvn test -Dtest=ArchitectureBoundaryGuardsTest` | N/A — no runtime behavior changes | `pom.xml`, guard test, and test fixtures only |

## Phase 1: Test-Only Dependency Resolution

## Phase 1: Test-Only Dependency Resolution

- [x] 1.1 Inspect the Spring Boot 4.1.0 parent effective dependency management; in `lc-ia-server/pom.xml` add test-scoped ArchUnit 1.5.0 and Jakarta Persistence API only. Omit the Jakarta version when managed; otherwise resolve and declare the minimal compatible version. Add no JPA starter, Hibernate runtime, or persistence infrastructure.

## Phase 2: Domain → Spring (strict TDD)

- [x] 2.1 Create the minimal Spring-dependent fixture under `lc-ia-server/src/test/java/com/leovinci/leos/architecturefixtures/` with the design selector `..domain..`; keep it outside production sources.
- [x] 2.2 In `ArchitectureBoundaryGuardsTest.java`, write and run the dedicated Spring rejection assertion first, while the relevant guard is absent; retain the focused failing (RED) command/output as behavior evidence.
- [x] 2.3 Add only the domain→Spring rule and its main-output-only evaluation; rerun the dedicated test and retain GREEN evidence.

## Phase 3: Domain → JPA/Hibernate (strict TDD)

- [x] 3.1 Create the minimal `..domain..` fixture with one `jakarta.persistence` or `org.hibernate` dependency, using the test-scoped API only.
- [x] 3.2 Write and run its dedicated rejection assertion while the relevant guard is absent; retain focused RED evidence.
- [x] 3.3 Add only the domain→JPA/Hibernate rule and main-output evaluation; rerun and retain GREEN evidence.

## Phase 4: Domain → Adapters (strict TDD)

- [x] 4.1 Create the minimal `..domain..` fixture and one dependent `..adapters..` fixture under `architecturefixtures/`; introduce no assumed package topology beyond those design selectors.
- [x] 4.2 Write and run the dedicated rejection assertion while the relevant guard is absent; retain focused RED evidence.
- [x] 4.3 Add only the domain→adapters rule and main-output evaluation; rerun and retain GREEN evidence.

## Phase 5: REST Inbound Adapter → Concrete Persistence Outbound Adapter (strict TDD)

- [x] 5.1 Create the minimal `..adapters.in.rest..` fixture dependent on a fixture selected by `..adapters.out.persistence..`, exactly as specified by the reconciled design.
- [x] 5.2 Write and run the dedicated rejection assertion and retain RED evidence. In addition to the initial absent-guard RED, remodel the fixture to the accepted `adapters.in.rest` / `adapters.out.persistence` topology and retain the semantic RED proving the previous `..controller..` / `..adapter.persistence..` selectors did not detect the intended violation.
- [x] 5.3 Add the minimal REST-inbound-adapter→concrete-persistence-outbound-adapter rule and its main-output evaluation; rerun and retain GREEN evidence.

## Phase 6: Consolidated Verification and Delivery Guard

- [x] 6.1 Keep production and fixture imports separate: import only `target/classes` for production and each fixture explicitly for its identical-rule rejection assertion; run `mvn test -Dtest=ArchitectureBoundaryGuardsTest` and retain all four GREEN results.
- [x] 6.2 Run `mvn test` in `lc-ia-server`; confirm discovery with `LcIaApplicationTest`, no CI workflow or production-role source changes, and no `synthetic-retrieval` changes.
- [x] 6.3 Inspect `git diff --stat` before the single PR; proceed at ≤400 authored changed lines, otherwise obtain `size:exception` before apply; retain RED/GREEN evidence with the work unit.
