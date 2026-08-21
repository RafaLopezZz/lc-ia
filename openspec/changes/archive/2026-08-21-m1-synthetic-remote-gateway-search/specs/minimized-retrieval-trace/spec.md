# Delta for minimized-retrieval-trace

## MODIFIED Requirements

### Requirement: Structured categorical trace

Each operation SHALL trace authorization, coverage, aggregate, and delivery categories using opaque synthetic tenant/gateway/operation/attempt IDs only. It SHALL NOT contain free reasoning, paths, file URIs, document content, source credentials, human tokens, bytes, real documents, or unauthorized tenant identities.  
(Previously: traces correlated authorized outcome categories and provenance.)

#### Scenario: Delivery trace

- GIVEN one logical operation with a redelivered attempt
- WHEN its trace is emitted
- THEN both attempts correlate to the one operation using opaque synthetic identifiers only

#### Scenario: Sensitive field

- GIVEN a trace input containing a prohibited value or cross-tenant identity
- WHEN trace validation runs
- THEN the value is absent and the trace reveals no unauthorized identity
