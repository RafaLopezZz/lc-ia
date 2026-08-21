# Design: Protect LC-IA Architecture Boundaries

## Technical Approach

Add one ordinary JUnit Jupiter test to `lc-ia-server`. It imports `target/classes` for production evaluation and imports each test fixture explicitly for its rejection proof. This keeps `src/main/java` and `src/test/java` bytecode separate: the sparse production server passes without invented roles, while four independent fixtures prove the exact same rules reject Spring, JPA/Hibernate, adapter, and controller-to-concrete-persistence-adapter dependencies.

`synthetic-retrieval` is excluded. M0.4 changes no CI workflow; `mvn test` is the M0.5-ready entry point.

## Architecture Decisions

| Decision                     | Choice                                                                                                                                                                                                                                                                                             | Alternatives considered                                                                                  | Rationale                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Role classification          | Infer protected roles from semantic package segments anywhere below `com.leovinci.leos`: `..domain..`, `..adapters..`, `..adapters.in.rest..`, and `..adapters.out.persistence..`. REST inbound adapters include controllers for this boundary; persistence implementations are outbound adapters. | Marker annotations; singular `..adapter..` / `..controller..` package conventions; defer classification. | This creates no production package now and follows the accepted capability topology `adapters.in.rest` / `adapters.out.persistence` without imposing a capability hierarchy. The first real class placed in one of these protected namespaces is automatically guarded. A controlled fixture demonstrated that the previous `..controller..` / `..adapter.persistence..` selectors did not recognize the intended topology. |
| Guard library                | `com.tngtech.archunit:archunit:1.5.0` with test scope. Use `ClassFileImporter` and `ArchRule.check(...)` inside ordinary `@Test` methods.                                                                                                                                                          | `archunit-junit5` or other `archunit-junit*` artifacts.                                                  | ArchUnit 1.5.0 is verified on Java 25. The existing `spring-boot-starter-test` already supplies JUnit Jupiter/Surefire discovery; `archunit-junit5` only adds ArchUnit-specific engine/annotation integration that these explicit checks do not use.                                                                                                                                                                        |
| JPA fixture API              | Add test-scoped `jakarta.persistence:jakarta.persistence-api`, version managed by the Spring Boot 4.1.0 parent. The fixture references `@Entity`.                                                                                                                                                  | Persistence starter; Hibernate runtime; functional database infrastructure.                              | The Jakarta Persistence API is the smallest compile-time type needed to prove a JPA/Hibernate-category dependency. It adds no persistence runtime or infrastructure and remains test-only.                                                                                                                                                                                                                                  |
| Production/fixture isolation | Import main output by `target/classes`; import fixture classes individually.                                                                                                                                                                                                                       | Import all classpath classes together.                                                                   | A fixture must never make the production result fail or turn a vacuous production pass into evidence.                                                                                                                                                                                                                                                                                                                       |

## Data Flow

```text
mvn test -> JUnit Jupiter guard test
         -> ClassFileImporter.importPath(target/classes) -> production rules
         -> import dedicated fixture -> identical rule -> assert AssertionError
```

The test builds reusable rules from the semantic package segments above. Domain classes must not depend on Spring packages, `jakarta.persistence`/`org.hibernate`, or classes under `..adapters..`. Classes in REST inbound adapters (`..adapters.in.rest..`), including controllers, must not directly depend on concrete persistence outbound adapters under `..adapters.out.persistence..`. Four isolated fixtures each create exactly one category violation and each assertion independently expects `ArchRule.check(...)` to throw.

## File Changes

| File                                                                               | Action | Description                                                                                                                                               |
| ---------------------------------------------------------------------------------- | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `lc-ia-server/pom.xml`                                                             | Modify | Add test-scoped ArchUnit 1.5.0 and managed Jakarta Persistence API only.                                                                                  |
| `lc-ia-server/src/test/java/com/leovinci/leos/ArchitectureBoundaryGuardsTest.java` | Create | Imports, role predicates, production checks, and four explicit rejection assertions.                                                                      |
| `lc-ia-server/src/test/java/com/leovinci/leos/architecturefixtures/`               | Create | Minimal isolated fixtures proving domain→Spring, domain→JPA/Hibernate, domain→adapters, and REST-inbound-adapter→persistence-outbound-adapter violations. |

No production source (including no fictitious `src/main/java` roles), runtime persistence, `synthetic-retrieval`, or CI workflow file changes are permitted. One work-unit commit contains dependencies, guard test, fixtures, and `mvn test` evidence; expected size is below the 800-line budget.

## Interfaces / Contracts

No runtime API is added. The test contract is:

```java
void springDomainFixtureIsRejected();
void jpaOrHibernateDomainFixtureIsRejected();
void adapterDomainFixtureIsRejected();
void controllerDependingOnPersistenceAdapterIsRejected();

void productionDomainDoesNotDependOnSpring();
void productionDomainDoesNotDependOnJpaOrHibernate();
void productionDomainDoesNotDependOnAdapters();
void productionControllersDoNotDependOnPersistenceAdapters();
```

Role convention is forward-compatible: only classes placed under the protected semantic segments `..domain..`, `..adapters..`, `..adapters.in.rest..`, or `..adapters.out.persistence..` participate in the applicable guards. Unrelated future capability packages remain unconstrained. Moving a class into one of these protected role namespaces makes the corresponding rule applicable on the next `mvn test` run.

## Testing Strategy

| Layer             | What to Test                                                      | Approach                                                                    |
| ----------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------- |
| Unit/architecture | Main output only passes all applicable rules.                     | Import `target/classes`; run via `mvn test`.                                |
| Unit/architecture | Every forbidden category is independently rejected.               | One fixture and one `assertThrows(AssertionError.class, ...)` per category. |
| Integration       | Maven discovers guards alongside the existing context smoke test. | `mvn test` in `lc-ia-server`.                                               |
| E2E/runtime       | N/A.                                                              | No runtime behavior or process boundary changes.                            |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. M0.5 may call the existing Maven command in CI without changing these guards.

## Open Questions

None.
