# ADR-0010: Expiry is given as a TTL; expired links return 410 Gone

- **Status:** Accepted (TTL format Proposed)
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Users may want links that stop working after a while. Clients need to tell "never existed" from "existed but expired".

## Decision

Accept an optional TTL on create and store an absolute `expires_at` (`now + TTL`); no TTL means the link never expires. After expiry, `GET` returns **410 Gone** (unknown codes return 404) and the entry is not cached.

**Open:** TTL format. Default is `ttlSeconds` (a positive integer); the alternative is an ISO-8601 duration such as `P7D`. No default or maximum TTL yet.

## Consequences

- Expired rows remain in the table; an index on `expires_at` allows a cleanup job later.
