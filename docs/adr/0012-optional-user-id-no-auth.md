# ADR-0012: Optional userId, no authentication yet

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

The spec says a URL is shortened "for a user" but does not define users or auth.

## Decision

Accept an optional `userId` string (max 64) on create and store it on the mapping. There is no authentication or authorization in this phase.

## Consequences

- Anyone can create links; ownership is informational only.
- Adding auth later (API key or JWT) is a new ADR.
