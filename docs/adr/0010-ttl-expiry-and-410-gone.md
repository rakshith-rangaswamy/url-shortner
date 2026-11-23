# ADR-0010: Expiry is given as a TTL; expired links return 410 Gone

- **Status:** Accepted
- **Date:** 2026-11-22 (TTL format decided 2026-11-23)
- **Deciders:** Rakshith

## Context

Users may want links that stop working after a while. Clients need to tell "never existed" from "existed but expired".

## Decision

Accept an optional TTL on create and store an absolute `expires_at` (`now + TTL`); no TTL means the link never expires. After expiry, `GET` returns **410 Gone** (unknown codes return 404) and the entry is not cached.

**TTL format: `ttlSeconds`**, a whole number of seconds from **60** to **315,360,000** (10 years), decided 2026-11-23. There is no default: omitting it means the link never expires. A number is the simplest for clients and matches Redis TTLs; the limits catch typos and absurd dates. Out-of-range values get 422. `expiresAt` is returned as an ISO-8601 timestamp.

## Consequences

- Expired rows remain in the table; an index on `expires_at` allows a cleanup job later.
