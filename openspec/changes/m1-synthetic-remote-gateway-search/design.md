# Design: M1 Synthetic Remote-Gateway Search

Build the synthetic-only vertical with `synthetic-retrieval` as a framework-neutral core and `lc-ia-server` as the only transport host. A gateway process initiates every HTTPS long poll. M1 does not select production identity, crypto, provider, persistence, document handling, or dependencies.

## Technical Approach

M1.1 adds the synthetic credential registry, controllable `Clock`, and the sole `(tenantId, idempotencyKey)` logical-Work authority. M1.2 queues the returned Work in memory with target-gateway selection, expiry, status, and deterministic ordering. M1.3 proves the gateway-initiated HTTPS long-poll path across two JVMs. M1.4 consolidates frozen contributions.

## Architecture Decisions

| Decision | Choice | Rejected / rationale |
|---|---|---|
| Shared boundary | `synthetic-retrieval` exposes only Java records, enums, interfaces, collections, and `java.time` types for ledger inputs/outputs. `lc-ia-server` converts HTTP messages at its edge. | Sharing Spring DTOs or server adapters would couple the synthetic core to transport. |
| Enforceable core boundary | `synthetic-retrieval` remains framework-free: its POM retains only JUnit test support and its production code MUST NOT reference Spring, JPA/Hibernate, or `lc-ia-server`/adapter packages. A JDK `jdeps`-based JUnit guard rejects those references. | Spring/ORM/server-adapter dependencies in the core violate the module boundary; ArchUnit is not added. |
| M1.3 transport | Use JDK `HttpsServer` in `lc-ia-server` for the remote adapter and JDK `HttpClient` in the gateway process. `ProcessBuilder` starts both mains with argument lists. | `spring-boot-starter-web`, client libraries, object calls, WebSocket, broker, tunnel, and inbound gateway ports are excluded. Existing Spring Boot remains unused by this adapter. |
| Dependency gate | No POM dependency is added. A minimal root reactor may wire the two existing modules so the server compiles against the existing core artifact. | If JDK `HttpsServer`/`HttpClient` cannot meet the proof, M1.4 is blocked pending an explicit maintainer-approved dependency amendment; it must not silently add one. |
| Synthetic trust | Use in-memory bound credentials, opaque fixture IDs, and `Clock`. | This is not production authentication, cryptography, enrollment, or security approval. |

The server module owns `adapters.in.https` and `adapters.out.gateway`; neither package is visible to the core. The shared port uses framework-neutral `Poll`, `Work`, `Result`, `Lease`, and categorical rejection types only. M0.4 server guards remain unchanged.

## Data Flow

```text
gateway JVM --HTTPS poll--> server HTTPS adapter -> core ledger
gateway JVM <-- lease/work --- server HTTPS adapter <- credential registry + Clock
gateway JVM --ACK/result--> server HTTPS adapter -> core ledger -> frozen aggregator
```

Remote validates tenant/gateway binding, active credential, issued-at/absolute expiry, nonce replay, version, idempotency, and remote authorization before leasing. The gateway repeats boundary checks plus local-final authorization; denial returns only a category. DTO conversion allows opaque IDs, categories, timestamps, protocol version, and candidate IDs only. It rejects paths, URIs, content, credentials, human tokens, documents, bytes, and cross-tenant identities. Traces contain only opaque IDs and categories.

## Interfaces / Contracts

```java
record Poll(TenantId tenant, GatewayId gateway, CredentialId credential, MessageId nonce,
            Instant issuedAt, Instant expiresAt, ProtocolVersion version) {}
record Work(OperationId operation, CorrelationId correlation, IdempotencyKey key,
            AttemptId attempt, List<SourceId> sources, Instant expiresAt, Instant leaseUntil) {}
record Result(TenantId tenant, GatewayId gateway, OperationId operation, CorrelationId correlation,
              AttemptId attempt, List<CandidateId> candidates, TerminalCategory category) {}
```

One tenant/idempotency key creates one logical operation and immutable `FrozenCoverage`; attempts are distinct. Compatible duplicates return accepted state; contradictory or post-expiry messages are rejected and never extend validity. Lease expiry/reconnect may issue another `AttemptId` for the same operation. Aggregation sorts gateway/source/candidate IDs; any frozen missing, denied, expired, incompatible, or non-terminal contribution is `PARTIAL`, otherwise `COMPLETE`.

## File Changes

| File | Action | Description |
|---|---|---|
| `pom.xml` | Create | Minimal reactor for the two existing modules; no external dependency addition. |
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/*` | Modify/Create | Framework-neutral ledger, trust model, boundary contracts, and frozen aggregation. |
| `synthetic-retrieval/src/test/java/.../SyntheticRemoteGatewayScenarioTest.java` | Create | M1.1–M1.3 scenarios and JDK dependency-boundary guard. |
| `lc-ia-server/pom.xml` | Modify | Reference the existing core module only; add no dependency. |
| `lc-ia-server/src/main/java/com/leovinci/leos/adapters/{in,out}/...` | Create | JDK HTTPS remote and gateway transport adapters. |
| `lc-ia-server/src/test/java/com/leovinci/leos/RemoteGatewayProcessE2ETest.java` | Create | Two-JVM HTTPS proof and transport isolation checks. |

## Testing Strategy and Slices

| Slice | RED tests before implementation | Rollback boundary |
|---|---|---|
| M1.1 | binding/revocation, Clock expiry, replay, idempotency, remote/local denial, prohibited DTO/trace data | core ledger/contracts |
| M1.2 | target gateway, tenant isolation, deterministic ordering, status lookup, absolute expiry | in-memory queue indexes |
| M1.3 | distinct child PIDs, gateway-originated HTTPS poll, no gateway listener, minimized round trip, no core framework/server references | reactor, JDK transport, process proof |
| M1.4 | fixture mutation after freeze, permutations, deterministic aggregation | frozen coverage/aggregator |

## Threat Matrix

| Boundary | Applicability | Design response / RED tests |
|---|---|---|
| Documentation-like paths | N/A — no executable classification | None |
| Git repository selection | N/A — no VCS operation | None |
| Commit state | N/A — no commit operation | None |
| Push state | N/A — no push operation | None |
| PR commands | N/A — no PR automation | None |

`ProcessBuilder` uses fixed executable paths and argument lists, never a shell or repository selection.

## Migration / Rollout

No migration or rollout. State, credentials, and TLS material are in-memory or test-temporary. Revert each slice independently; M1.4 cannot land without M1.1–M1.3.

## Open Questions

- [ ] M1 remains synthetic-only and does not close any production-security gate.
