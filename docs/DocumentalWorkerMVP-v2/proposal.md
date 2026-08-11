# Proposal: DocumentalWorkerMVP-v2

## Intent

Regenerate the DocumentalWorker MVP spec set from the enriched requirements doc (`docs/analisis-requisitos-documentalworker-mvp-enriquecido.md`), replacing the single-capability `documental-worker-lifecycle` spec with a multi-capability contract that mirrors the real workflow: **policy check → document.search → structured answer → evidence validation → respond/escalate**. This change **supersedes** the prior incomplete change `DocumentalWorkers` (Fases 1-2 de 4 aplicadas; verify-report says not archivable). Existing Spring Boot code is **foundation, not throwaway**.

## Scope

### In Scope

13 core capabilities (enriched doc §7-§8, §10-§12), each becomes `openspec/specs/<name>/spec.md`:

- `tenant-management` (RF0, §7 RF0)
- `worker-identity-and-kill-switch` (RF1, RF7 kill switch, §7)
- `worker-run-lifecycle` (RF2, §7)
- `worker-step-traceability` (RF3, §7)
- `document-corpus-management` (RF4, §7)
- `tool-gateway-and-document-search` (RF5, RF13 tool control, §7)
- `llm-client-contract` (RF6, §7)
- `structured-answer-contract` (RF7, §7)
- `evidence-validation` (RF8, §7)
- `policy-engine` (RF9, P-001..P-012, §7)
- `human-escalation` (RF10, §7)
- `audit-traceability` (RF11, §7)
- `cost-and-observability` (RNF3, RNF7, §8)
- `data-minimization-and-retention` (RNF2, §8)

Progressive optional capabilities (decision #1): `document-classification` (RF14) and `field-extraction` (RF15) — kept, not dropped; always with sources+validation+audit.

### Out of Scope (non-goals, enriched doc §3.2)

Advanced admin panel; multi-worker collaboration; dynamic worker registry; memory service; ERP writes; automatic email sending; MailboxWorker / ReportingWorker; L3/L4 automation; distributed event bus; separate microservices; external vector database (Phase 1); complex agent orchestration; fine-tuning; full corporate permissions system; any tool that modifies external systems in MVP (RF13); real RLS in first slice (only "preparation"); real LLM/RAG quality, vector DB, advanced UI, dynamic registry.

## Capabilities

### New Capabilities
- See "In Scope" — 13 core + 2 progressive optional, all new full specs.

### Modified Capabilities
- None at delta level (no prior specs exist in `openspec/specs/`); prior `documental-worker-lifecycle` change spec is superseded wholesale.

## Approach

Fresh multi-capability spec set authored from the enriched doc. Supersede prior `DocumentalWorkers` change (incomplete application, not archivable). Keep existing code as foundation; OpenSpec delta model applies only if a capability later needs modification. Policy decisions stay deterministic and outside the LLM (§1.2). Tenant isolation is a security frontier, not a DB column (§1.4). Cost is real from day 1 (decision #4).

## Supersedes

- `DocumentalWorkers` (prior change, incomplete): Fases 1-2 of 4 applied, verify-report declares non-archivable. Existing code under `aiworkers/` is **reused as foundation**, not discarded. The single-capability spec `documental-worker-lifecycle` is replaced by the 13+2 capability set.

## Decisions (all 12 closed — encoded verbatim)

| # | Decision | Source ref |
|---|----------|-----------|
| 1 | RF14 (classification) + RF15 (field extraction) kept as progressive optional capabilities per real CU, always with sources+validation+audit. Never dropped. | enriched doc §7 (RF14/RF15) |
| 2 | NEW change that **supersedes** prior incomplete `DocumentalWorkers`. Existing code conserved as base. | enriched doc §0; prior verify-report |
| 3 | Policy rules MVP: P-001..P-012 complete (correct inconsistency from doc — P-006/P-009 disambiguated by decisions #5/#7). | enriched doc §7 RF9 (L622-640) |
| 4 | Real cost from day 1. LlmClient returns tokens, ToolGateway returns tool cost, persisted in AuditEvent. No stub 0. "Cost vs savings" question lives inside Phase 1. | enriched doc §8 RNF7 (L899) |
| 5 | Invalid JSON after evidence validation: always ESCALATE with reason=invalid_answer. Never automatic FAILED from parser. | enriched doc §7 RF9 P-006 (L629); §12 CL10 |
| 6 | P-007/P-008/P-009 detection (tension "LLM does not decide"): hybrid — regex + LLM judge suggests, but PolicyEngine always can deterministic override. Deterministic override wins. | enriched doc §1.2 (L56-62); §7 RF9 |
| 7 | P-006 branch: BLOCK on clear attempt to modify rules / access other tenants / run unauthorized tools / ignore policies. ESCALATE on inconclusive suspicion. Always AuditEvent. | enriched doc §7 RF9; §12 CL5/CL9 |
| 8 | SLA HumanEscalation MVP: HIGH=4h laborables, MEDIUM=1 día laboral, LOW=2 días laborables. Expiry => SLA_BREACHED + block auto-close as success. | enriched doc §7 RF10 (L686-691) |
| 9 | RLS preparation: tenant_id NOT NULL on all tables + indexes. Real RLS policies in Phase 2. MVP isolates via app query. | enriched doc §8 RNF1 (L773) |
| 10 | API auth tenant_id: client does NOT freely decide tenant. tenant_id resolved from auth/secure context. In local dev may use header X-Tenant-Id but ONLY in dev profile. | enriched doc §8 RNF1; §13 API |
| 11 | Default escalation owner: tenant.defaultEscalationOwner if exists; else ESCALATE stays UNASSIGNED + emits configuration_required. Never invent user. | enriched doc §7 RF10; §5 actores |
| 12 | RVB1-3: encode as validation entity/config of the pilot (process, success metric, stop criterion). Not just markdown. | enriched doc §6 (L207-243) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| 13+ specs exceed review budget (chained PRs needed) | High | Trigger `ask-always` chained-PR strategy; slice specs into review-sized units. |
| Spec sprawl vs. enriched doc drift | Med | Cite doc line numbers in each spec; one source of truth. |
| Supersede leaves orphaned prior change folder | Med | Keep prior folder for traceability; archive only after v2 verifies. |
| RF14/RF15 scope creep | Med | Mark progressive-optional; gate behind real CU. |

## Rollback Plan

Delete `openspec/changes/DocumentalWorkerMVP-v2/` directory. No code touched in propose phase. Prior `DocumentalWorkers` folder remains untouched as fallback reference.

## Dependencies

- Enriched doc `docs/analisis-requisitos-documentalworker-mvp-enriquecido.md` as canonical source.
- Existing `aiworkers/` Spring Boot codebase as implementation foundation.

## Success Criteria

- [ ] 13 core + 2 progressive-optional capability specs authored, each citing enriched doc sections.
- [ ] All 12 decisions traceable to spec requirements.
- [ ] Prior `DocumentalWorkers` change explicitly superseded (not silently abandoned).
- [ ] No out-of-scope item leaks into Phase 1 specs.

## Size Estimate (chained-PR budget check)

Conservative forecast: ~13 core specs × ~50-80 lines + 2 progressive × ~40 lines + design + tasks ≈ **900-1100 changed lines total**. **Exceeds 400-line review budget** → orchestrator MUST trigger `ask-always` chained-PR strategy; slice by capability clusters (e.g., lifecycle cluster, policy cluster, infra cluster).
