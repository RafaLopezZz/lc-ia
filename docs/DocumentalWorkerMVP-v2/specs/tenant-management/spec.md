# Tenant Management Specification

Capability: `tenant-management`
Description: Tenant representation, lifecycle, and isolation preparation (RLS prep, tenant_id NOT NULL + indexes). Tenant is a security frontier, not a DB column.
Supersedes: This change (`DocumentalWorkerMVP-v2`) supersedes the prior `DocumentalWorkers` change's `documental-worker-lifecycle` spec elements related to tenant scoping.

> Source: enriched doc §7 RF0 (L248-271); §8 RNF1 (L773-789). Decisions #9 (RLS prep), #10 (API auth tenant_id from secure context).

## ADDED Requirements

### Requirement: Tenant Entity and Lifecycle

The system MUST represent each tenant as a `Tenant` entity with fields `id`, `name`, `status`, `default_escalation_owner`, `created_at`, `updated_at`. `status` MUST be one of `ACTIVE`, `PAUSED`, `DISABLED`. Every `WorkerRun`, `WorkerStep`, `DocumentSource`, `DocumentChunk`, `AuditEvent`, and `HumanEscalation` MUST belong to exactly one tenant.

#### Scenario: Run cannot exist without tenant

- GIVEN no tenant is resolved for the request
- WHEN the system attempts to create a `WorkerRun`
- THEN the creation is rejected
- AND no `WorkerRun` row is persisted

#### Scenario: Paused tenant blocks runs

- GIVEN a tenant whose `status` is `PAUSED` or `DISABLED`
- WHEN a new run is requested for any worker of that tenant
- THEN the run is `BLOCKED` by policy P-002
- AND an `AuditEvent` of type `RUN_BLOCKED` is emitted
> Source: doc §7 RF0 (L260-270); §7 RF9 P-002 (L625).

### Requirement: tenant_id NOT NULL and Indexes (RLS Preparation)

Every tenant-scoped table MUST define `tenant_id` as `NOT NULL` with a database-level constraint, and MUST have an index on `tenant_id` (composite where a secondary key exists, e.g. `(tenant_id, worker_run_id)`). Real row-level security (RLS) policies are a non-goal of this spec set and are deferred to Phase 2.

#### Scenario: NOT NULL constraint enforced

- GIVEN a migration that attempts to insert a tenant-scoped row with a null `tenant_id`
- WHEN the insert executes
- THEN the database rejects the insert with a constraint violation
- AND the error is surfaced as a controlled failure, not a silent default

#### Scenario: Cross-tenant retrieval is blocked at query layer

- GIVEN two tenants A and B each owning documents
- WHEN tenant A performs a `document.search`
- THEN results contain only documents where `tenant_id = A`
- AND an automated isolation test proves tenant A cannot retrieve tenant B content
> Source: doc §8 RNF1 (L775-788); decision #9.

### Requirement: Tenant Resolution from Secure Context (API Auth)

The `tenant_id` for any API request MUST be resolved from authentication or a secure server-side context, NEVER from client-supplied request data in production. The header `X-Tenant-Id` MAY be honored ONLY when the active profile is `dev`. In production profile, any client-supplied `tenant_id` (body field, query param, or header) MUST be rejected.

#### Scenario: Client-supplied tenant_id rejected in prod

- GIVEN the application runs under the `prod` profile
- WHEN a request includes a `tenantId` body field or `X-Tenant-Id` header
- THEN the request is rejected with a controlled error
- AND the tenant is resolved only from the authenticated principal

#### Scenario: X-Tenant-Id accepted only in dev

- GIVEN the application runs under the `dev` profile
- WHEN a request includes `X-Tenant-Id` matching an `ACTIVE` tenant
- THEN the tenant is resolved from that header
- AND the resolution is recorded in the audit metadata
> Source: doc §8 RNF1 (L773-789); §13 (L1135); decision #10.
