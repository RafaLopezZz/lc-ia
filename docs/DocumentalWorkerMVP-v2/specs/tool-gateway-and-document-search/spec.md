# Tool Gateway and Document Search Specification

Capability: `tool-gateway-and-document-search`
Description: The `ToolGateway` as the single execution path for tools, the `document.search` contract, and the MVP rule that no external-system-modifying tool is allowed.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning tool execution.

> Source: enriched doc §7 RF5 (L442-485); §7 RF13 (L749-768); §14 (L1229-1245).

## ADDED Requirements

### Requirement: ToolGateway is the Only Tool Execution Path

Every tool invocation MUST execute through the `ToolGateway`. Direct tool execution from the LLM or domain logic MUST NOT occur. Each call MUST produce a `WorkerStep` of type `TOOL_CALL` and an `AuditEvent` of type `TOOL_CALLED`.

#### Scenario: Tool call is gated

- GIVEN the worker requests a tool
- WHEN the gateway receives the request
- THEN policy P-003 is evaluated against `allowed_tools`
- AND only allowed tools execute
> Source: doc §7 RF5 (L481-484).

### Requirement: document.search Contract

The `document.search` tool MUST accept `{ tenantId, query, limit, filters: { documentType?, dateFrom?, dateTo? } }` and MUST return `{ results: [ { documentId, chunkId, title, sourceName, documentDate, score, excerpt } ] }`. `tenantId` MUST be enforced to the requesting tenant; a client-supplied `tenantId` differing from the principal MUST be rejected.

#### Scenario: Search returns tenant-scoped results

- GIVEN an active tenant with matching chunks
- WHEN `document.search` runs with a query
- THEN only chunks of the requesting tenant are returned
- AND each result includes `documentId`, `chunkId`, `sourceName`, and `excerpt`
> Source: doc §7 RF5 (L448-477).

### Requirement: Unauthorized Tool is Blocked

If the requested tool is not in `WorkerIdentity.allowed_tools`, the gateway MUST block the call (policy P-003), emit a `RUN_BLOCKED` audit event, and skip LLM/execution.

#### Scenario: Tool not in allowed_tools

- GIVEN a worker whose `allowed_tools = ["document.search"]`
- WHEN a run requests `ticket.create`
- THEN the call is blocked
- AND a `TOOL_CALL` step with `status=BLOCKED` and an audit event are recorded
> Source: doc §7 RF5 (L483); §7 RF9 P-003 (L626); §12 CL9 (L1115).

### Requirement: No External-Modifying Tools in MVP

The MVP MUST NOT allow any tool that modifies external systems (e.g. `ticket.create`, `email.draft`, `erp.read` writes). Only read-only `document.search` is permitted.

#### Scenario: Modifying tool rejected

- GIVEN the MVP configuration
- WHEN a run requests any write/modifying tool
- THEN the tool is blocked regardless of `allowed_tools` content
- AND the block is audited
> Source: doc §7 RF13 (L765-768).
