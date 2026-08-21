# In-Memory Work Queue Specification

## Purpose

Provide framework-neutral, in-memory availability and tenant-isolated lookup for M1.1-authorized synthetic Work. This capability does not define transport, recovery, or results.

## Requirements

### Requirement: Targeted pending-work delivery

The system MUST make pending Work available only to its addressed gateway within its owning tenant. A delivery query MUST return only unexpired Work eligible for that tenant and target gateway. It MUST NOT make Work available to another tenant or gateway.

#### Scenario: RED A — delivers work only to its target gateway

- GIVEN pending unexpired Work for gateway A in tenant T
- WHEN gateway A and gateway B each query tenant T
- THEN only gateway A receives that Work

#### Scenario: Tenant isolation during delivery

- GIVEN pending unexpired Work for tenant T1 and gateway A
- WHEN gateway A queries as tenant T2
- THEN no Work owned by T1 is returned

### Requirement: Boundary-owned logical-operation identity

`SyntheticTrustBoundary` MUST remain the sole authority that creates or reuses Work for a `(tenant, idempotencyKey)` pair. The queue MUST consume the Work returned by that authority and MUST NOT create, re-register, or maintain an independent logical-operation idempotency map.

#### Scenario: RED B — reuses one tenant-scoped logical operation

- GIVEN the boundary accepts two equivalent requests for tenant T and idempotency key K
- WHEN their returned Work is indexed by the queue
- THEN both references identify exactly one logical operation
- AND the queue adds no second operation for `(T, K)`

#### Scenario: M1.1 authority non-regression

- GIVEN a boundary-created Work is provided to the queue
- WHEN the queue records or makes it available
- THEN the Work identity and boundary idempotency result remain unchanged

### Requirement: Absolute expiry and controlled time

The system MUST evaluate eligibility using an injected controllable `Clock`. Expiry MUST be absolute: Work expired at query time MUST NOT be returned, and queue activity MUST NOT extend or revive its validity.

#### Scenario: RED C — never delivers expired Work

- GIVEN Work that expires at instant E and a controlled clock at E
- WHEN its addressed gateway queries the queue
- THEN the Work is not returned
- AND advancing or repeating queries does not make it eligible

### Requirement: Tenant-scoped correlation status

The system MUST provide status lookup by correlation ID only within the owning tenant. A lookup for an unknown correlation ID or a correlation ID owned by another tenant MUST reveal no Work status.

#### Scenario: RED D — returns status only within the owning tenant

- GIVEN indexed Work with correlation C owned by tenant T1
- WHEN T1 and T2 each look up C
- THEN T1 receives its status and T2 receives no status

### Requirement: Deterministic eligible-work ordering

The system MUST return eligible Work in ascending queue acceptance order, after tenant, target-gateway, and expiry filtering. Repeated equivalent queries MUST preserve that order. The system MUST NOT rely on unspecified map iteration order.

#### Scenario: RED E — deterministic ordering without cross-tenant leakage

- GIVEN interleaved eligible Work for tenant T1 and tenant T2, including two Work items for T1 gateway A
- WHEN gateway A queries tenant T1 repeatedly
- THEN it receives only T1's Work in their queue acceptance order
- AND each equivalent query preserves that order

### Requirement: M1.2 boundary

The system MUST remain in-memory and framework-neutral. It MUST NOT introduce HTTP, polling, WebSocket, lease, ACK, redelivery, reconnect, retry, gateway results, `COMPLETE`/`PARTIAL`, persistence, brokers, dependencies, real retrieval, or production-security behavior.

#### Scenario: Queue-only capability boundary

- GIVEN an M1.2 queue operation
- WHEN it makes eligible Work available or returns correlation status
- THEN no transport, recovery, result, or persistence semantic is required
