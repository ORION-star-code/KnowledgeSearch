# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Enterprise knowledge-base search and CDC synchronization platform. Spring Boot 3.x + Java 21 application where MySQL is the source of truth and Elasticsearch is a derived search index. Canal subscribes to MySQL binlog changes and a Sync Worker rebuilds and writes ES documents.

## Commands

```bash
mvn clean package          # Build
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run a specific test class
mvn spring-boot:run        # Run locally
```

## Architecture

```
MySQL (source of truth)
    │ Binlog
    ▼
Canal Server
    │
    ▼
Sync Worker ──────► Elasticsearch (search replica)
```

### Key Architecture Rules
- **MySQL is the only source of truth** - business writes always persist to MySQL first
- **ES is a derived index** - never trust partial binlog fields alone; always re-query MySQL when rebuilding ES documents
- **Sync flows**: Canal events → Sync Worker → rebuild from MySQL → upsert/delete ES
- **Failure handling**: failures are logged to `sync_fail_log` with retry context; scheduled retry jobs attempt compensation
- **Eventual consistency** - not strong consistency

### V1 Scope (default)
article CRUD, publish/offline, category/tag management, keyword search across title/summary/content, category/tag filters, highlight results, full sync, incremental sync, sync failure logging, scheduled retry/compensation.

V2 enhancements (RabbitMQ, Redis, IK analyzer, search suggestions, hot-word stats) are **not** in scope unless explicitly requested.

## Repository Structure

| Path | Purpose |
|------|---------|
| `src/main/java/.../controller/admin/` | Admin APIs (article/category/tag/sync) |
| `src/main/java/.../controller/search/` | Public search API |
| `src/main/java/.../service/` | Business logic layer |
| `src/main/java/.../domain/entity/` | MySQL entity models |
| `src/main/java/.../mapper/` | MyBatis-Plus mapper interfaces |
| `src/main/java/.../search/` | ES document model, query builder, ES gateway abstraction |
| `src/main/java/.../sync/` | canal event adapter, sync worker, retry scheduler |
| `src/main/java/.../common/` | Shared responses, enums, exceptions |

## Data Sync Rules

When rebuilding ES documents:
- `kb_article` changes → rebuild full article doc
- `kb_article_tag` changes → reload full tag set, rebuild article
- `kb_tag` changes → find impacted articles, rebuild affected docs
- `kb_category` changes → find articles in category, rebuild affected docs

Full sync must be: paginated, idempotent, safe to re-run, only sync published/non-deleted content.

## Search Rules

Default strategy (V1 scope, intentionally fixed): `multi_match(title^3, summary^2, content^1)` with bool filters on categoryId/status/tagNames, highlight on title/summary/content, sort by `_score desc` + `publishTime desc`.

ES index name: `kb_article_index`. Do not replace with vector search, semantic search, or custom analyzers unless explicitly requested — this is a V1 architecture boundary, not a temporary limitation.

## Three Core Operational Paths

| Path | Trigger | Key Components |
|------|---------|----------------|
| **Admin writes** | Article/category/tag API | AdminArticleController → ArticleServiceImpl → MySQL only |
| **CDC sync** | Canal binlog events | CanalSyncListener → IncrementalSyncServiceImpl → ArticleSyncWorker → rebuild from MySQL → ES |
| **Search query** | Search API | SearchController → KnowledgeSearchServiceImpl → ArticleIndexRepository → ES |

## Safe Change Policy

Prefer minimal diffs, local consistency, incremental refactors. Avoid large rewrites, mixing business write logic with ES logic, bypassing retry/compensation mechanisms.

## More Details

See `AGENTS.md` for comprehensive architecture rules, coding conventions, testing expectations, and common task guides.
