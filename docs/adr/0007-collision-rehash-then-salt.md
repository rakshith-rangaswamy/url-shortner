# ADR-0007: Collision handling: re-hash up to 5 times, then salt or counter

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

A truncated 7-character hash can collide with an existing code for a different URL.

## Decision

On collision, re-hash up to **5** times (`shortener.maxRehashAttempts`). If it still collides, append a salt or counter to the input and hash again. The `UNIQUE` index on `short_code` is the final guard: a concurrent insert that loses the race is treated as a collision and the next candidate is tried.

## Consequences

- Uniqueness holds even across multiple app instances.
- The policy lives in `ShortCodeGenerator.candidate(url, attempt)`. `UrlShortenerService` tries up to 20 candidates, then answers 503 (practically unreachable).
- The insert itself is the uniqueness check (ADR-0019): there is no separate "check, then insert" step to race.
