# Remote Work Registration Specification

## Purpose

Provide framework-neutral, in-memory registration of synthetic remote work before any gateway poll. The registry/queue owns operation identity and tenant-scoped idempotency; polling only validates trust/protocol and reads eligible work.

## Requirements

### Requirement: Service-first PENDING registration

The system MUST expose a service-first `register` operation independent of `Accepted` and polling. The registry/queue MUST create a registry-generated operation ID and PENDING work before any poll, retaining tenant, target gateway, idempotency key, correlation ID, expiresAt, delivery state, and status.

#### Scenario: Registers PENDING work before any gateway poll

- GIVEN a synthetic registration request and no gateway poll
- WHEN the service invokes `register`
- THEN one PENDING work record with a registry-generated operation ID is available
- AND all supplied identity, routing, correlation, and expiry fields are retained

### Requirement: Tenant-scoped idempotent registration

The registry/queue MUST reuse one operation for equivalent `(tenant, idempotencyKey)` registrations and MUST NOT reuse it across tenants.

#### Scenario: Reuses only the tenant-scoped operation

- GIVEN repeated registrations with key K for tenant T and one registration with K for tenant U
- WHEN `register` is invoked for each request
- THEN T receives the same operation ID and U receives a different operation ID

### Requirement: Eligible delivery with controlled time

The in-memory registry/queue MUST use an injected controllable Clock, return only PENDING unexpired work for its owning tenant and target gateway, and MUST preserve deterministic registration order.

#### Scenario: Delivers only targeted pending work

- GIVEN unexpired PENDING work for tenant T, gateway A, and gateway B
- WHEN gateway A reads eligible work for T
- THEN only A's work is returned

#### Scenario: Excludes expired work absolutely

- GIVEN work expiring at instant E and a controlled Clock at E
- WHEN its target gateway reads eligible work
- THEN the work is not returned and later reads do not revive it

#### Scenario: Preserves deterministic eligible-work order

- GIVEN interleaved eligible registrations, including two for tenant T and gateway A
- WHEN A repeatedly reads eligible work for T
- THEN only T/A work is returned in the same registration order

### Requirement: Tenant-scoped correlation status

The registry/queue MUST expose a work status by correlation ID only to its owning tenant; an unknown or cross-tenant lookup MUST reveal no status.

#### Scenario: Restricts correlation status to its tenant

- GIVEN PENDING work with correlation C owned by tenant T
- WHEN T and another tenant look up C
- THEN only T receives PENDING status

### Requirement: Validation-only polling and M1.1 regression

`SyntheticTrustBoundary.authorize(Poll)` MUST validate trust/protocol only and MUST NOT create, register, reuse, or otherwise cause remote work. Polling MUST depend on neither `Accepted` nor authorization-side work creation.

#### Scenario: M1.1 authorization creates no remote work

- GIVEN no prior registration and a valid gateway poll
- WHEN `SyntheticTrustBoundary.authorize(Poll)` is invoked
- THEN no remote work exists or is registered as a side effect

## Strict RED Evidence Gate

Before any production implementation, each six registration scenarios and the M1.1 regression MUST have a RED record containing: exact scenario/test identifier; written UTC timestamp and immutable source snapshot; pre-RED production snapshot stating the registration API is absent; focused command; RED start/end UTC timestamps; non-zero exit code and full-output SHA-256/reference; direct missing-API failure assertion; and proof the evidence predates production changes. Incomplete evidence MUST NOT admit GREEN work. The implementation MUST remain in-memory and framework-neutral and MUST NOT add persistence, HTTP/API delivery, leases, ACKs, redelivery, reconnect, results, brokers, or any M1.3+ behavior.

## Result Contract

This artifact defines requirements only. It makes no claim that RED evidence exists, that tests were written or run, or that implementation has started.
