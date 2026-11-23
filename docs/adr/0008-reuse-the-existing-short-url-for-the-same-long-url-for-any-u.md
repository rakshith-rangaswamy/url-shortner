# ADR-0008: Reuse the existing short URL for the same long URL, for any user

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Shortening the same URL repeatedly could fill the table with duplicates. The owner decided reuse applies to the **same URL**, regardless of which user asks.

## Decision

Before generating a code, look for an unexpired, non-custom mapping with the same `long_url_hash` **and** the same full `long_url` (so a hash collision can never return the wrong link), oldest first. If found, return it with **200 OK**; otherwise create one and return **201 Created** with a `Location` header. Both are documented in OpenAPI.

## Consequences

- The `userId` on a reused mapping is the original creator's.
- **Trade-off:** two concurrent requests for the same new URL can both insert. Both codes work, and later lookups converge on the oldest row. Accepted under eventual consistency (ADR-0016).
- Custom aliases are never reused this way.
