# AGENTS.md

## Project Overview

This repository implements an enterprise knowledge-base search and CDC synchronization platform.

Current primary architecture:
- MySQL is the source of truth.
- Elasticsearch is a read/search replica.
- Canal subscribes to MySQL binlog changes.
- Sync Worker rebuilds and writes ES documents.
- Failed sync operations must be retryable, traceable, and compensatable.

Current implementation priority:
- V1 is the default target: Canal -> Sync Worker -> Elasticsearch
- V2 enhancements such as RabbitMQ, Redis, IK analyzer, search suggestions, and hot-word stats are optional and should not be introduced unless the task explicitly requires them.

## Primary Goals for Agents

When modifying this repo, optimize for:
1. correctness
2. consistency between MySQL and ES
3. minimal, reviewable diffs
4. preserving current architecture boundaries
5. testability and observability

Do not optimize for premature abstraction.

---

## Architecture Rules

### Source of truth
- MySQL is the only source of truth.
- Elasticsearch is a derived search index only.
- Business write flows must persist to MySQL first.

### Sync rules
- Do not introduce direct dual-write logic to MySQL and Elasticsearch in normal business handlers.
- Incremental sync should flow through Canal-derived events and Sync Worker processing.
- When building or rebuilding an ES document, do not trust partial binlog fields alone.
- Always re-query MySQL and related data when needed, then rebuild the ES document.

### Data rebuild rules
For article-related ES documents:
- article changes: rebuild the full article document
- article-tag relation changes: reload full tag set, then rebuild
- tag changes: find impacted articles and rebuild/update affected ES docs
- category changes: find impacted articles and rebuild/update affected ES docs

### Consistency model
- The system uses eventual consistency, not strong consistency.
- Sync failures must be recorded and retried.
- Compensation jobs are part of the default architecture, not optional polish.

---

## Current Feature Scope

Agents should assume the current default scope includes:
- article CRUD
- publish/offline flows
- category management
- tag management
- keyword search
- multi-field search across title/summary/content
- category/tag filters
- highlight results
- full sync
- incremental sync
- sync failure logging
- scheduled retry / compensation
- basic sync status querying

Do not add V2 features unless explicitly requested.

---

## Repository Map

- `src/main/java/com/knowledge/search/KnowledgeSearchApplication.java`
  - Spring Boot startup entrypoint
  - enables scheduling and mapper scanning
- `src/main/java/com/knowledge/search/controller`
  - `admin` admin APIs for article/category/tag/sync
  - `search` public search API
- `src/main/java/com/knowledge/search/service`
  - `article`, `category`, `tag`, `search`, `sync` service contracts and placeholder implementations
- `src/main/java/com/knowledge/search/domain/entity`
  - MySQL entity models for article/category/tag/article_tag/sync_fail_log
- `src/main/java/com/knowledge/search/mapper`
  - MyBatis-Plus mapper interfaces
- `src/main/java/com/knowledge/search/search`
  - ES document model, document builder, query object, ES gateway abstraction
- `src/main/java/com/knowledge/search/sync`
  - `canal` event abstraction and adapter
  - `worker` sync worker
  - `retry` scheduled retry job
- `src/main/java/com/knowledge/search/common`
  - common response models, enums, exceptions, base entity
- `src/main/java/com/knowledge/search/config`
  - OpenAPI and application properties configuration
- `src/main/resources`
  - `application.yml`
  - `application-dev.yml`
- `src/test/java/com/knowledge/search`
  - Spring Boot context-load test

If you add a new module, keep naming aligned with existing package conventions.

---

## Coding Conventions

### General
- Follow existing package structure and naming before introducing new abstractions.
- Prefer small focused service methods over large “万能” service classes.
- Keep controller thin.
- Put business orchestration in service layer.
- Put sync-specific logic in sync layer, not in admin/search controller logic.

### API changes
When adding or changing an API:
- update request/response DTOs
- update validation
- update controller/service logic
- update OpenAPI / Knife4j annotations if present
- add or update tests

### Elasticsearch changes
When changing ES mapping, query strategy, or document structure:
- check impact on full sync
- check impact on incremental sync
- check compatibility with existing indexed data
- prefer additive changes over breaking mapping changes

### Database changes
For schema changes:
- prefer backward-compatible migrations
- consider sync compatibility
- consider rollback
- update related rebuild/conversion logic

---

## Search Rules

Default search behavior should follow existing project intent:
- title has highest relevance weight
- summary has medium weight
- content has lower weight
- support category/status/tag filters
- support highlight
- default sort should preserve relevance first, then recency where applicable

Do not replace the current search strategy with vector search, semantic search, or custom analyzers unless explicitly requested.

---

## Sync and Compensation Rules

### Full sync
If touching full sync logic:
- support paginated processing
- keep it idempotent
- allow repeated execution safely
- only sync eligible published, non-deleted content unless requirements change

### Incremental sync
If touching incremental sync:
- prefer rebuild from MySQL snapshot over partial field patching
- keep delete/offline semantics explicit
- ensure logs contain enough context to retry

### Failure handling
If a sync action fails:
- persist a retryable failure record
- include business id, business type, payload/context, and error message
- do not silently swallow exceptions

### Retry jobs
- retries should be bounded
- repeated failures should become visible for manual handling
- do not create infinite retry loops

---

## Testing Expectations

For any non-trivial change, agents should add or update tests.

Priority:
1. unit tests for converters / builders / service logic
2. integration tests for API behavior
3. integration tests for sync rebuild logic where practical

When changing:
- article CRUD -> test persistence and publish/offline behavior
- search -> test query conditions, filters, highlights, and sort behavior
- sync -> test full rebuild and at least one incremental path
- retry -> test failure logging and retry state transitions

Do not claim a feature is complete without validating the most relevant path.

---

## Safe Change Policy

Prefer:
- minimal diffs
- local consistency with existing code
- incremental refactors
- explicit logs around sync flows

Avoid:
- large architectural rewrites
- introducing MQ in V1 tasks
- mixing business write logic with ES write logic
- bypassing existing retry/compensation mechanisms
- speculative abstraction without current usage

---

## Common Tasks

### Implementing a new admin field
When adding a new article field:
1. update MySQL entity / schema if needed
2. update DTOs and validation
3. update CRUD service logic
4. update ES document mapping/converter if searchable or displayable
5. update full sync and incremental rebuild logic
6. add tests

### Modifying sync behavior
When changing sync logic:
1. identify source table(s)
2. identify affected ES document(s)
3. rebuild from MySQL + related tables where needed
4. update retry/failure handling if behavior changes
5. verify full sync still works

### Adding search conditions
When adding a search filter:
1. update request model
2. update ES query builder
3. update API docs
4. add tests for the filter alone and combined conditions

---

## Commands

### Build
- `mvn clean package`

### Run tests
- `mvn test`

### Run app locally
- `mvn spring-boot:run`

If a command fails, inspect the repo before inventing a new command.

---

## When Unsure

If requirements are ambiguous:
- infer from nearby code first
- preserve V1 architecture by default
- prefer MySQL -> rebuild -> ES over shortcut sync logic
- document assumptions in the final summary

If a requested change conflicts with these rules, explain the conflict clearly before implementing.
