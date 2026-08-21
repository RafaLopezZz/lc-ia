# Design: Bootstrap LC-IA Server Scaffold

## Technical Approach

Create an independent `lc-ia-server` Maven project, not a reactor child. Its only direct runtime starter dependency is `spring-boot-starter`; omitting web and infrastructure starters makes Boot select a non-web context. `LcIaApplication` delegates to standard `SpringApplication.run`; after successful initialization, `main()` returns. With no runtime work keeping the JVM alive, JVM termination invokes Boot's registered shutdown hook to close the context gracefully. This satisfies `application-bootstrap` without lifecycle code.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
| --- | --- | --- | --- |
| Module boundary | New standalone `lc-ia-server/` | Root reactor; modify `synthetic-retrieval` | Preserves existing module independence and limits rollback to one directory. |
| Boot baseline | Spring Boot `4.1.0` parent, Java release `25`, Boot Maven plugin | Hand-managed dependency/plugin versions; older Boot line | Boot 4.1 supports Java 25 and Maven 3.9.6 exceeds its Maven minimum; the parent supplies aligned dependency and plugin management. |
| Application mode | `spring-boot-starter` only; standard `SpringApplication.run` | Web starter; explicit lifecycle, runners, keep-alives, exit calls | No web classes are present, so Boot creates a non-web context and naturally terminates after initialization. |
| Verification | One `@SpringBootTest(webEnvironment = NONE)` smoke test plus packaged-JAR process check | HTTP test; custom shutdown test | Proves context creation and non-web launch without introducing a runtime contract. |

## Data Flow

There is no request, persistence, or infrastructure data flow in this baseline.

    java -jar
       -> LcIaApplication.main
       -> SpringApplication.run
       -> non-web ApplicationContext
       -> main() returns
       -> no runtime work keeps JVM alive
       -> JVM termination invokes Boot shutdown hook
       -> ApplicationContext closes gracefully -> process exit 0

## File Changes

| File | Action | Description |
| --- | --- | --- |
| `lc-ia-server/pom.xml` | Create | Standalone Maven build, Java 25, Boot parent, core/test starters, and executable-JAR plugin. |
| `lc-ia-server/src/main/java/com/leovinci/leos/LcIaApplication.java` | Create | `@SpringBootApplication` entry point using standard Boot startup. |
| `lc-ia-server/src/test/java/com/leovinci/leos/LcIaApplicationTest.java` | Create | Non-web context-load smoke test. |
| `openspec/changes/bootstrap-lcia-server-scaffold/design.md` | Create | This implementation design. |

## Interfaces / Contracts

The sole public executable contract is:

```java
package com.leovinci.leos;

@SpringBootApplication
public class LcIaApplication {
    public static void main(String[] args) {
        SpringApplication.run(LcIaApplication.class, args);
    }
}
```

No HTTP, domain, persistence, gateway, authentication, or lifecycle interfaces are introduced.

## Testing Strategy

| Layer | What to Test | Approach |
| --- | --- | --- |
| Unit | N/A | The entry point has no branch or domain logic. |
| Integration | Context boots as non-web without services | `mvn test` runs `@SpringBootTest(webEnvironment = NONE)`. |
| Runtime | Executable JAR initializes and exits naturally | `mvn package`, then `java -jar target/lc-ia-server-0.0.1-SNAPSHOT.jar`; require exit code 0. |

The single work-unit commit includes module, smoke test, and build/runtime evidence. Estimated authored change is under 150 lines, within the requested single-PR 800-line budget. Rollback removes only `lc-ia-server/` and this change artifact.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. The runtime command is verification-only, not an application integration boundary.

## Migration / Rollout

No migration or rollout required. The module is isolated and has no deployed consumers.

## Open Questions

None.
