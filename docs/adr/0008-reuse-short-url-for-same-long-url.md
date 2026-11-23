# ADR-0008: Reuse the existing short URL for the same long URL, for any user

- **Status:** Superseded by ADR-0020 (2026-11-23)
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Shortening the same URL repeatedly could fill the table with duplicates. The owner decided reuse applies to the **same URL**, regardless of which user asks.

## Decision

Before generating a code, look for an unexpired, non-custom mapping with the same `long_url_hash` **and** the same full `long_url` (so a hash collision can never return the wrong link), oldest first. If found, return it with **200 OK**; otherwise create one and return **201 Created** with a `Location` header. Both are documented in OpenAPI.

## Consequences

- The `userId` on a reused mapping is the original creator's.
- Concurrent requests for the same new URL compute the same first candidate, so one insert wins and the other hits a duplicate key; it then sees that the row holding the code is the same URL and returns it with 200. No duplicate is created in that case.
- The new request's TTL and userId are ignored on reuse (ADR-0019).
- Custom aliases are never reused this way.
