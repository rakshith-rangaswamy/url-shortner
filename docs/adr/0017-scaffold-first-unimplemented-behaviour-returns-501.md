# ADR-0017: Scaffold first: unimplemented behaviour returns 501

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

The owner asked for the full structure first and business logic later, and confirmed that the cache-miss read path should stay a documented stub for now.

## Decision

Wire every layer end to end, document the planned flow on each stub, and throw `NotImplementedYetException` (mapped to **501 Not Implemented**) where logic is missing. Validation, reserved aliases, health checks and config checks are real.

## Consequences

- The app runs and can be tested today.
- Each feature is implemented by replacing a stub and enabling its disabled tests.
