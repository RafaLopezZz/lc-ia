# Synthetic Remote-Gateway Search Specification

## Purpose

Synthetic search gate.

## Requirements

### Requirement: M1.1 Synthetic trust and search boundary

The system SHALL provide a non-production in-memory synthetic boundary across the HTTP process limit. Each request SHALL bind tenant/gateway, revocable credential, correlation/operation IDs, and controllable-Clock issued-at/absolute expiry; revoked, mismatched, expired, replayed, or invalid requests SHALL be rejected. Tenant-operation idempotency SHALL preserve a logical search through one-or-more delivery attempts. Remote and local-final authorization SHALL succeed; local denial prevails. Boundary payloads/outputs/traces SHALL contain only opaque synthetic IDs/minimized candidate metadata, never paths, file URIs, document content, source credentials, human tokens, bytes, real documents, or unauthorized tenant identities. This is not production-security approval.

#### Scenario: Authorized operation

- GIVEN a bound, unrevoked tenant/gateway with both authorizations
- WHEN remote issues a pre-expiry search
- THEN one correlated operation accepts minimized output

#### Scenario: Boundary rejection

- GIVEN revocation, tenant mismatch, replay, expiry, or prohibited payload
- WHEN either process validates the request
- THEN it is rejected without crossing prohibited data or unauthorized identity

### Requirement: M1.2 In-memory work queue

The system SHALL provide a framework-neutral in-memory queue for Work created or reused only by the M1.1 trust boundary. It SHALL select unexpired work only for its target gateway, preserve one logical operation for each `(tenant, idempotencyKey)`, and expose correlation-ID status only within the owning tenant. The queue SHALL use an injected Clock and deterministic ordering among eligible work. It SHALL NOT create Work, extend expiry, lease, ACK, redeliver, retry, reconnect, accept gateway results, or aggregate outcomes.

#### Scenario: Targeted eligible delivery

- GIVEN unexpired pending work for multiple tenants and target gateways
- WHEN one gateway requests work for its tenant
- THEN only its eligible work is returned in deterministic order

#### Scenario: Idempotency, expiry, and status isolation

- GIVEN boundary-created Work with a tenant-scoped idempotency key, correlation ID, and absolute expiry
- WHEN the same logical operation is queued again, time passes expiry, or another tenant requests its status
- THEN no duplicate Work is created, expired work is not delivered, and cross-tenant status is unavailable

### Requirement: M1.3 Independent-process long-poll proof

The system SHALL prove the flow with actual remote and gateway processes. The gateway SHALL initiate outbound HTTPS long-polling; the proof SHALL NOT use in-process calls, inbound ports, tunnels, or documents/bytes.

#### Scenario: Process proof

- GIVEN independently started remote and gateway processes
- WHEN the gateway opens an outbound HTTPS long poll and receives a valid synthetic search
- THEN the minimized result returns through that transport and separation is observable
