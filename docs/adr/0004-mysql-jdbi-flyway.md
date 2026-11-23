# ADR-0004: MySQL as the source of truth, accessed with JDBI and Flyway

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Short URL mappings must be stored durably, looked up by short code, and checked for uniqueness.

## Decision

Store mappings in **MySQL 8.4**, table `url_mapping` (`short_code` unique and case-sensitive via `utf8mb4_bin`, `long_url`, `long_url_hash`, `user_id`, `is_custom`, `created_at`, `expires_at`). Access it with **JDBI 3** SQL Objects (`UrlMappingRepository`) on Dropwizard's connection pool. Manage the schema with **Flyway** migrations in `src/main/resources/db/migration`, applied at startup. `docker-compose.yml` provides MySQL for local runs.

## Consequences

- The `UNIQUE` index on `short_code` is the final guarantee of uniqueness (see ADR-0007).
- SQL is written by hand; no ORM.
- Read replicas can be added later for read scaling and availability (see ADR-0016).
