# Document Corpus Management Specification

Capability: `document-corpus-management`
Description: `DocumentSource` and `DocumentChunk` entities, tenant scoping, soft delete, and the invariant that answers never cite sourceless documents.
Supersedes: prior `DocumentalWorkers` `documental-worker-lifecycle` spec elements concerning document corpus.

> Source: enriched doc §7 RF4 (L396-439); §8 RNF2 (L792-808); §14 (L1229-1245).

## ADDED Requirements

### Requirement: DocumentSource Entity

The system MUST persist documents as `DocumentSource` with fields `id`, `tenant_id`, `title`, `source_type`, `source_uri`, `document_date`, `version_label`, `checksum`, `status`, `created_at`, `updated_at`, `deleted_at`. `status` MUST be one of `ACTIVE`, `ARCHIVED`, `DELETED`. Every `DocumentSource` MUST belong to exactly one tenant.

#### Scenario: Document is tenant-scoped

- GIVEN a document created for tenant A
- WHEN tenant B performs a search
- THEN the document is never returned
- AND no metadata of the document leaks to tenant B
> Source: doc §7 RF4 (L435); §8 RNF1 (L775-788).

### Requirement: DocumentChunk Entity

The system MUST persist fragments as `DocumentChunk` with fields `id`, `tenant_id`, `document_source_id`, `chunk_index`, `content`, `content_summary`, `metadata`, `created_at`, `deleted_at`. Deleted chunks MUST NOT be returned by any search or cited in any answer.

#### Scenario: Deleted chunk is not retrievable

- GIVEN a chunk whose `deleted_at` is set
- WHEN a `document.search` is executed for that tenant
- THEN the chunk is excluded from results
- AND reindexing/invalidation occurs on document deletion
> Source: doc §7 RF4 (L437); §8 RNF2 (L803); §12 CL13 (L1119).

### Requirement: No Sourceless Citations

A response MUST NOT cite a source without a `document_source_id`. When a document is deleted but still indexed, the chunk MUST be invalidated or reindexed before any answer uses it.

#### Scenario: Answer never cites without source id

- GIVEN an LLM produces a source reference lacking `documentId`
- WHEN evidence validation runs
- THEN the answer is rejected as insufficient evidence
- AND the run escalates
> Source: doc §7 RF4 (L438); §12 CL1 (L1107); §12 CL13 (L1119).

### Requirement: Phase-1 Search in PostgreSQL

Phase 1 search MUST use PostgreSQL `ILIKE` or basic full-text search with mandatory `tenant_id` filter and simple ranking. External vector DB and pgvector are non-goals of this spec (Phase 2/3).

#### Scenario: Basic search filters by tenant

- GIVEN chunks for tenants A and B
- WHEN `document.search` runs for tenant A
- THEN only tenant A chunks are returned, ranked by simple relevance
> Source: doc §14 (L1231-1245).
