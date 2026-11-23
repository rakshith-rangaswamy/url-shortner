# ADR-0018: Click analytics are out of scope for now (no-op tracker)

- **Status:** Accepted (2026-11-23)
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

302 redirects make click counting possible (ADR-0011), but it has not been decided whether counting is in scope for this phase or where counts would be stored.

## Decision

**Click counting is out of scope for this phase.** Nothing in the spec reads click counts, and counting without a way to see them adds load for no value. A `ClickTracker` interface is called on each redirect and wired with a **no-op** implementation, so counting can be added later without touching the redirect code.

## Consequences

- Adding counting later means a real `ClickTracker` (for example Redis `INCR` or an async queue) without touching the redirect code.
- Scope and storage get their own ADR when counting is needed.
