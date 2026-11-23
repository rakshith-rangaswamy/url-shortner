# ADR-0019: Insert-first uniqueness, expired codes stay taken, reuse ignores new TTL

- **Status:** Accepted
- **Date:** 2026-11-23
- **Deciders:** Rakshith

## Context

Implementing the shortening flow (replacing the scaffold of ADR-0017) raised three questions the earlier ADRs left open: how to check that a code is free without races, what happens to codes whose link has expired, and what to do when an existing URL is shortened again with a different TTL or user.

## Decision

1. **Insert first.** Uniqueness is checked by inserting the row and relying on the `UNIQUE` index on `short_code`. A duplicate-key error (MySQL 1062) means the code is taken: for a generated code the next candidate is tried (ADR-0007); for a custom alias the answer is 409 with suggestions (ADR-0009). There is no separate "is it free?" query before the insert.
2. **Expired codes stay taken.** An expired row keeps its code, for generated codes and custom aliases alike. A short URL therefore never starts pointing somewhere else. Shortening the same URL again after expiry gives a new code.
3. **Reuse returns the existing link as is.** When an unexpired generated short URL exists for the URL, it is returned with 200 and the new request's `ttlSeconds` is ignored. Since ADR-0020 reuse only happens for the same user, so the owner never changes.
4. **New links are written to Redis immediately**, so the first redirect is a cache hit.

## Consequences

- No race between check and insert; concurrent requests for the same URL converge on one code (ADR-0008).
- Expired rows are never reused, so the table grows until a cleanup job archives them (future ADR); 62^7 codes leave ample room.
- A client that wants a different expiry for an already-shortened URL has to use a custom alias.
- ADR-0017 (unimplemented behaviour returns 501) is superseded: every endpoint is implemented.
