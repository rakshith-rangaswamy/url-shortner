# ADR-0011: Redirect with 302 Found

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

301 lets browsers cache the redirect, which cuts load but hides later clicks from the service. The owner wants click counts available for analytics and cache tuning.

## Decision

Respond to `GET /{shortCode}` with **302 Found** and a `Location` header.

## Consequences

- Every click reaches the service, so it can be counted (ADR-0018) and kept hot in Redis.
- More traffic than 301, which the cache is there to absorb (ADR-0005).
