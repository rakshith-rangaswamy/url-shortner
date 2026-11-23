# ADR-0015: End-to-end tests with Testcontainers; target behaviour kept as disabled tests

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Integration tests should run against the real database and cache, and the agreed behaviour should be captured before it is built.

## Decision

End-to-end tests (`*IT`) start **MySQL and Redis with Testcontainers** and the whole app with `DropwizardAppExtension`, and call it over HTTP. A separate test runs the app with Redis unreachable. Behaviour that is agreed but not yet implemented is written as `@Disabled` tests that are switched on as each feature lands. Unit tests cover validation, alias policy and config limits.

## Consequences

- `mvn verify` needs Docker.
- Disabled tests double as an executable spec.
