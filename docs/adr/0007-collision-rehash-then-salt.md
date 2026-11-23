# ADR-0007: Collision handling: re-hash up to 5 times, then salt or counter

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

A truncated 7-character hash can collide with an existing code for a different URL.

## Decision

On collision, re-hash up to **5** times (`shortener.maxRehashAttempts`). If it still collides, append a salt or counter to the input and hash again. The `UNIQUE` index on `short_code` is the final guard: a concurrent insert that loses the race is retried as a collision.

## Consequences

- Uniqueness holds even across multiple app instances.
- The policy lives in `ShortCodeGenerator.candidate(url, user, attempt)`; tests are written and disabled until it is implemented.
