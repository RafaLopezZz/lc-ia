## Exploration: protect-lcia-architecture-boundaries

### Current State

`lc-ia-server` is the production application under construction. It is an independent Java 25 Spring Boot non-web module with one bootstrap class, `com.leovinci.leos.LcIaApplication`, and one `@SpringBootTest` context smoke test. Its current production surface contains no domain, persistence, controller, or adapter classes.

`synthetic-retrieval` is an independent synthetic laboratory. It is not the production server domain/core and MUST NOT define or substitute for the future DDD/hexagonal boundaries of `lc-ia-server`. The documented target is a modular hexagonal monolith: domain code is independent from Spring and infrastructure, ports are inward-facing contracts, and controllers are inbound adapters rather than persistence clients.

M0.4 needs executable rules in the actual `lc-ia-server` build. A green rule against the presently sparse production set is necessary but insufficient: each rule must also be evaluated against a controlled violating fixture under `src/test/java`, proving that the same rule fails when its forbidden dependency is introduced. No fake production packages or production placeholder classes are justified.

### Affected Areas

- `lc-ia-server/pom.xml` — add the test-scoped architecture-test dependency and ensure the guard executes through `mvn test`; do not pin a version unless the selected parent or dependency-management evidence requires it.
- `lc-ia-server/src/test/java/...` — define the architecture rules, evaluate them against real production classes, and use minimal test-only violating fixtures to prove detection.
- `lc-ia-server/src/main/java/com/leovinci/leos/LcIaApplication.java` — remains the only current production class; it is the real production import set for the initial guard.
- `synthetic-retrieval/` — explicitly out of scope for M0.4 architecture enforcement.

### Approaches

1. **Production architecture rules with controlled test fixtures** — add architecture tests to `lc-ia-server`; import the real production classes for the production checks, then evaluate each identical rule against a dedicated test fixture that deliberately violates it.
   - Pros: protects the real production module; proves every guard can fail before the corresponding production layer exists; runs with `mvn test`; adds no fake production topology; leaves future package and predicate detail to the implementation/design phase.
   - Cons: needs a test-scoped architecture dependency and carefully isolated fixtures so they are never treated as production code.
   - Effort: Low

2. **Production-only architecture rules** — add the same rules but evaluate only the current production classes.
   - Pros: smallest initial test code.
   - Cons: produces vacuous passes for absent domain, controllers, persistence adapters, and references; does not meet the M0.4 proof requirement.
   - Effort: Low

3. **Maven dependency bans as the primary guard** — ban selected dependencies in the Maven model.
   - Pros: useful later for an actual module-level boundary.
   - Cons: cannot establish source-level DDD/hexagonal rules or prove controlled violations; there is no relevant production module boundary to justify it for M0.4.
   - Effort: Low

### Recommendation

Choose Approach 1 for `lc-ia-server` only. Define four source-level architecture constraints: domain code must not depend on Spring; domain code must not depend on JPA/Hibernate; domain code must not reference adapters; and controllers must not directly depend on concrete persistence adapters. Execute the rules as normal Maven tests against the real `src/main/java` classes.

For each constraint, place the smallest deliberately violating class under `src/test/java` and assert that evaluating the identical rule against that fixture fails. The fixtures are evidence of guard sensitivity, not production scaffolding, and must not be imported into the production check. Keep rule expression, package predicates, package hierarchy, and the architecture-library version open for the design/proposal phase. Maven dependency bans may be considered only as complementary future protection when a real module boundary exists; they do not replace these M0.4 guards.

This is one reviewable work unit: architecture test setup, production-rule execution, and controlled-failure fixtures must ship together. It should remain well below the 800-line review budget and map to one conventional commit with its `mvn test` verification.

### Risks

- Test fixtures can accidentally make a rule pass or fail for the wrong reason if production and fixture imports are not explicitly separated.
- The future production package topology is not implemented; prematurely fixing package names or predicates would fossilize a speculative structure.
- Architecture tests enforce bytecode dependencies, not runtime behavior or Maven module relationships; add complementary dependency bans only once a real module boundary warrants them.
- The current OpenSpec configuration predates the server scaffold and reports unavailable testing; implementation evidence must use the actual `lc-ia-server` `mvn test` command.

### Ready for Proposal

Yes — propose M0.4 as `lc-ia-server` architecture guardrails with real-production checks and test-only controlled violations. Keep `synthetic-retrieval`, fake production classes, package-topology decisions, and Maven dependency bans out of the primary scope.
