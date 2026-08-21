# Delta for synthetic-retrieval-outcomes

## MODIFIED Requirements

### Requirement: Immutable expected coverage and deterministic aggregation

The system SHALL freeze authorized expected gateways, sources, mandatory status, and configuration before fan-out. `COMPLETE` SHALL require every frozen gateway's valid terminal contribution; every other outcome SHALL be `PARTIAL` without shrinking the snapshot. Consolidation SHALL be deterministic despite arrival/redelivery order.  
(Previously: coverage required immutable expected contributions.)

#### Scenario: Frozen coverage

- GIVEN a search whose expected coverage is frozen
- WHEN configuration, availability, or delivery order later changes
- THEN aggregation uses the original expected coverage and deterministic result

#### Scenario: Complete

- GIVEN every expected gateway supplies a valid terminal contribution
- WHEN the result is consolidated
- THEN coverage is `COMPLETE`

#### Scenario: Partial

- GIVEN an expected gateway is absent, denied, expired, incompatible, or lacks a valid terminal contribution
- WHEN the result is consolidated
- THEN coverage is `PARTIAL` and the snapshot is not reduced
