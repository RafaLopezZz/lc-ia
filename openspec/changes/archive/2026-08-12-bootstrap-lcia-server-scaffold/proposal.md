# Proposal: Bootstrap LC-IA Server Scaffold

## Intent

Provide developers with an executable, isolated LC-IA server baseline. It must prove Spring Boot can initialize and terminate cleanly without operational infrastructure or an HTTP contract.

## Scope

### In Scope

- A standalone `lc-ia-server` Maven module with Spring Boot executable-JAR packaging.
- `com.leovinci.leos.LcIaApplication` and a context-load smoke test.
- Non-web initialization and natural clean termination verified with `java -jar`.

### Out of Scope

- Changes to `synthetic-retrieval`, a root Maven reactor, or CI configuration (M0.5).
- HTTP endpoints, health checks, workers, runners, keep-alives, custom lifecycle mechanisms, JPA, PostgreSQL, Auth0, gateways, LLMs, real data, and future-domain placeholders.

## Capabilities

### New Capabilities

- `application-bootstrap`: Isolated non-web Spring Boot bootstrap, executable packaging, and natural clean termination.

### Modified Capabilities

None. Existing synthetic retrieval capabilities remain unchanged.

## Approach

Create the smallest standalone non-web Spring Boot module: core starter, test starter, Boot Maven plugin, one application class, and one context smoke test. Preserve Java 25 and Maven 3.9.6; pin and validate a compatible Spring Boot version. Standard Boot logs and the native non-web lifecycle are sufficient—do not add `System.exit`, `SpringApplication.exit`, runners, keep-alives, or custom startup/shutdown behavior.

## Affected Areas

| Area                                                                    | Impact    | Description                                   |
| ----------------------------------------------------------------------- | --------- | --------------------------------------------- |
| `lc-ia-server/pom.xml`                                                  | New       | Standalone build and executable-JAR boundary. |
| `lc-ia-server/src/main/java/com/leovinci/leos/LcIaApplication.java`     | New       | Non-web application entry point.              |
| `lc-ia-server/src/test/java/com/leovinci/leos/LcIaApplicationTest.java` | New       | Context-load smoke test.                      |
| `synthetic-retrieval/`                                                  | Unchanged | Preserved independent synthetic laboratory.   |

## Risks

| Risk                              | Likelihood | Mitigation                                             |
| --------------------------------- | ---------- | ------------------------------------------------------ |
| Premature infrastructure coupling | Low        | Exclude integrations and speculative extension points. |
| Java 25/Boot incompatibility      | Low        | Pin a compatible Boot version; validate all commands.  |

## Rollback Plan

Remove only `lc-ia-server/`; no existing module, workflow, or capability changes.

## Dependencies

- Accepted Java 25 and Maven 3.9.6 toolchain; no network service or external infrastructure at runtime.

## Success Criteria

- [ ] `mvn test` succeeds for `lc-ia-server` and loads the Spring Boot context without external services.
- [ ] `mvn package` succeeds for `lc-ia-server` and produces an executable Spring Boot JAR.
- [ ] `java -jar` initializes successfully without external infrastructure and then terminates naturally and cleanly.
- [ ] `synthetic-retrieval` has no functional modifications.
- [ ] The change remains a single work-unit PR within the approved 800 changed-line budget.
