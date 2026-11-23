# ADR-0012: Optional userId, no authentication yet

- **Status:** Accepted (amended 2026-11-23 by ADR-0020: userId is now required)
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

The spec says a URL is shortened "for a user" but does not define users or auth.

## Decision

~~Accept an optional `userId` string (max 64)~~. Since ADR-0020, `userId` is **required** on create: 1 to 64 letters, digits, `-`, `_` or `.`, sent in the JSON body. There is still no authentication or authorization; the caller is trusted to send its own id.

## Consequences

- Anyone can create links under any userId until authentication exists; ownership drives reuse (ADR-0020) but is not enforced.
- Adding auth later (API key or JWT) is a new ADR.
