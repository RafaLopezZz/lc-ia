# Tasks: Bootstrap LC-IA Server Scaffold

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 90–140 authored lines |
| 400-line budget risk | Low |
| 800-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | One work-unit commit in a single PR |
| Delivery strategy | single-pr |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Deliver the isolated non-web executable baseline and its smoke test. | Single PR | `mvn test` in `lc-ia-server` exits 0 | `mvn package` then `java -jar target/lc-ia-server-0.0.1-SNAPSHOT.jar` in `lc-ia-server`; both exit 0 and JAR returns naturally | Remove `lc-ia-server/` only; leave existing modules and SDD artifacts unchanged |

## Phase 1: Isolated Module Foundation

- [x] 1.1 Create `lc-ia-server/pom.xml` as an independent Maven project using Spring Boot `4.1.0`, Java release `25`, `spring-boot-starter`, `spring-boot-starter-test`, and the Boot Maven plugin; do not add a root reactor, web, or infrastructure dependencies.
- [x] 1.2 Create `lc-ia-server/src/main/java/com/leovinci/leos/LcIaApplication.java` with `@SpringBootApplication` and only `SpringApplication.run(LcIaApplication.class, args)` in `main`; add no runner, exit call, keep-alive, or lifecycle code.

## Phase 2: Non-Web Context Proof

- [x] 2.1 Create `lc-ia-server/src/test/java/com/leovinci/leos/LcIaApplicationTest.java` with `@SpringBootTest(webEnvironment = NONE)` to prove the context loads with no external services or HTTP contract.
- [x] 2.2 Run `mvn test` from `lc-ia-server`; record successful exit status and the context-smoke result.

## Phase 3: Packaging and Natural-Exit Verification

- [x] 3.1 Run `mvn package` from `lc-ia-server`; verify it exits 0 and produces `target/lc-ia-server-0.0.1-SNAPSHOT.jar`.
- [x] 3.2 Run `java -jar target/lc-ia-server-0.0.1-SNAPSHOT.jar` from `lc-ia-server`; verify successful initialization, natural clean exit code 0, and no HTTP server or external-service requirement.
- [x] 3.3 Review the final diff: only `lc-ia-server/` plus this change's SDD artifacts may change; exclude CI, root reactor, lifecycle code, HTTP, infrastructure, and `synthetic-retrieval` functional edits; confirm authored additions plus deletions are at most 800.
