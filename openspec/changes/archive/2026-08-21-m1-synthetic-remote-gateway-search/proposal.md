# Proposal: M1 Synthetic Remote-Gateway Search

## Intent

Prove a synthetic search-only remote↔gateway vertical with two real processes and gateway-initiated outbound HTTPS long-polling. Synthetic protocol gate, not production-ready. The local gateway remains final authority. [Remote contract §4, §6, §8](../../../docs/LC-IA-MVP-contrato-remoto-gateways-v0.1.md); [remote/local architecture §6](../../../docs/LC-IA-MVP-arquitectura-remota-local-v0.1.md).

## Scope

### In Scope
- **M1.1:** in-memory, non-production trust/protocol boundary: tenant–gateway binding, revocable synthetic credential, correlation, controllable `Clock` issued-at/absolute expiry, anti-replay, idempotency, minimized results, and one logical search separated from delivery attempts.
- **M1.2:** framework-neutral in-memory queue: target-gateway-only selection, tenant-scoped idempotency, absolute expiry, correlation-status lookup, and deterministic eligible-work ordering.
- **M1.3:** E2E gate with true remote and gateway processes over gateway-initiated HTTPS long-polling, never object calls in one JVM.
- **M1.4:** deterministic aggregation and frozen expected coverage.
- Remote and independent local authorization; only synthetic, opaque candidate metadata crosses the boundary.

### Out of Scope
- Production controls, real credentials/identities, human tokens, production protocol/crypto/provider/framework choices, persistence, workflows, dependencies, or infrastructure.
- Filesystem paths, file URIs, document content, source credentials, real documents, browser download, transfers, or bytes.
- Any documentation-only child ticket; each M1 slice produces executable behavior.

## Capabilities

### New Capabilities
- `synthetic-remote-gateway-search`: synthetic trust boundary, in-memory queue, two-process long-poll search, and local-final authorization.

### Modified Capabilities
- `synthetic-retrieval-outcomes`: consolidate remote gateway contributions into deterministic `COMPLETE`/`PARTIAL` coverage.
- `minimized-retrieval-trace`: correlate protocol and delivery state using only synthetic opaque identifiers and safe categories.

## Approach

Build M1.1 first; add the in-memory queue (M1.2), prove the two-process long-poll path (M1.3), then aggregate frozen contributions (M1.4). Preserve tenant-scoped authorization and conservative results. [authorized scope spec](../../specs/authorized-scope-resolution/spec.md); [outcomes spec](../../specs/synthetic-retrieval-outcomes/spec.md); [trace spec](../../specs/minimized-retrieval-trace/spec.md).

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `synthetic-retrieval/` | Modified | Synthetic authorization, outcomes, and consolidation. |
| `lc-ia-server/` | Modified | Future remote process, subject to specs/design. |
| `openspec/specs/` | Modified/New | Define the M1 contract before implementation. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Synthetic boundary mistaken for production security | Medium | Label it non-production; defer real-control decisions. |
| In-process shortcut invalidates the gate | Medium | Require independent processes in M1.4. |
| Duplicate delivery changes logical results | Medium | Separate operation identity from attempts and enforce compatible idempotency. |

## Rollback Plan

Revert the single M1 PR; no real data, credentials, persistent state, or external infrastructure is introduced.

## Dependencies

- Accepted M1 specs and design; no new runtime dependency is proposed.

## Success Criteria

- [ ] M1.1–M1.4 each deliver executable behavior; none is documentation-only.
- [ ] M1.3 runs remote and gateway as separate processes using outbound HTTPS long-polling.
- [ ] Both boundaries authorize; local denial prevails; outputs remain minimized and synthetic.
- [ ] Coverage and aggregate results remain deterministic and never claim production security readiness.
