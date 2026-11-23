# ADR-0018: Click analytics: no-op tracker for now

- **Status:** Proposed
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

302 redirects make click counting possible (ADR-0011), but it has not been decided whether counting is in scope for this phase or where counts would be stored.

## Decision

Define a `ClickTracker` interface called on each redirect, wired with a **no-op** implementation.

## Consequences

- Adding counting later means a real `ClickTracker` (for example Redis `INCR` or an async queue) without touching the redirect code.
- Decide scope and storage in a follow-up ADR.
