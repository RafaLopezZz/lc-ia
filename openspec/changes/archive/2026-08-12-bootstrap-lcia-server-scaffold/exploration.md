# Exploration: `bootstrap-lcia-server-scaffold`

### Current State

The repository has one independent Maven module, `synthetic-retrieval`, which is a synthetic-only Java 25/JUnit 5 laboratory. Its tests pass locally (31 tests) and its CI workflow runs only that module. There is no LC-IA server module, root Maven reactor, Spring Boot dependency, HTTP API, persistence layer, or external integration.

The imported M0.3.1 backlog item explicitly requests a separate Spring Boot application with root package `com.leovinci.leos`, a context smoke test, an executable JAR, and a controlled CLI lifecycle. It explicitly excludes JPA, PostgreSQL, Auth0, REST controllers, gateways, LLMs, and empty future-domain packages. The local toolchain is JDK 25.0.1 and Maven 3.9.6; current Spring Boot documentation supports Java 17 through 26 and Maven 3.6.3+.

### Affected Areas

- `lcia-server/pom.xml` — new, standalone Spring Boot Maven module and executable-JAR packaging boundary.
- `lcia-server/src/main/java/com/leovinci/leos/LcIaApplication.java` — new minimal application entry point under the required root package.
- `lcia-server/src/test/java/com/leovinci/leos/LcIaApplicationTest.java` — new context-load smoke test.
- `synthetic-retrieval/` — unchanged; remains the separately runnable synthetic-contract laboratory.
- `.github/workflows/synthetic-retrieval.yml` — unchanged in this change; it is intentionally scoped only to the existing laboratory.
- `openspec/config.yaml` — records `auto-chain`, but the explicit session preflight for this change overrides delivery to `single-pr` with an 800-line review budget; no config edit is needed during exploration.

### Approaches

1. **Standalone non-web Spring Boot module** — add one `lcia-server` Maven module using `spring-boot-starter`, `spring-boot-starter-test`, the Boot Maven plugin, one `@SpringBootApplication`, and one context smoke test. Configure it as non-web so the packaged JAR starts and exits without a port or external service.
   - Pros: exactly satisfies the M0.3.1 acceptance criteria; isolates the real server from the synthetic laboratory; creates no HTTP surface or infrastructure dependency; comfortably fits one small PR.
   - Cons: introduces the first Spring Boot version choice and a second Maven invocation path until a future root reactor is deliberately introduced.
   - Effort: Low

2. **Root Maven reactor plus server module** — add an aggregator `pom.xml` that builds both `lcia-server` and `synthetic-retrieval` together.
   - Pros: one command can eventually build all modules.
   - Cons: changes the current independent laboratory build, creates parent/version-management decisions, and exceeds the ticket's minimum bootstrap scope without proving an immediate need.
   - Effort: Medium

3. **Web server scaffold with a health endpoint** — add `spring-boot-starter-web` and a controller.
   - Pros: proves an HTTP listener immediately.
   - Cons: contradicts the explicit exclusion of REST controllers, starts a persistent process rather than proving controlled termination, and creates an API contract before a real use case exists.
   - Effort: Medium

### Recommendation

Use Approach 1. Create a separate `lcia-server` module with only the Spring Boot core and test starter, the required `com.leovinci.leos` application class, one `@SpringBootTest` context smoke test, and executable-JAR packaging. Run the JAR with `--spring.main.web-application-type=none` (or make the non-web mode its fixed default) so the runtime proof starts and returns without external infrastructure.

Keep the module standalone rather than adding a root reactor, controllers, a health endpoint, database setup, identity integration, or placeholder domain packages. Forecast: approximately 70–120 authored changed lines in one work-unit commit, well below the 800-line single-PR budget. The implementation work unit must include `mvn test`, `mvn package`, and a `java -jar` controlled-exit check; rollback is removal of the new `lcia-server/` directory only.

### Risks

- The current `openspec/config.yaml` says `auto-chain`, while this change's explicit session preflight says `single-pr`; downstream artifacts must retain the explicit session decision rather than silently following stale configuration.
- JDK 25 is available and documented as compatible with current Spring Boot, but the exact Spring Boot version should be pinned in the proposal/design and validated by the new module's package and runtime checks.
- A successful context smoke test proves only bootstrap wiring; it does not validate the remote/local trust boundaries, identity, tenant authorization, document access, or any production readiness claim.
- The existing CI workflow does not cover the future server module. CI expansion belongs to M0.5 unless the proposal explicitly changes that boundary.

### Ready for Proposal

Yes. Proceed with a proposal that fixes the standalone non-web module boundary, preserves `synthetic-retrieval` unchanged, and limits the implementation to executable bootstrap plus context and controlled-exit verification. No product decision is needed before proposal; Spring Boot version pinning is a technical detail to validate in design/apply.
