# ADR-0006: Short codes are a hash of the long URL, Base62-encoded, 7 characters

- **Status:** Accepted (hash function Proposed)
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Each short URL needs a unique, compact code. A global counter would need coordination across instances; the owner chose hashing to avoid that complexity.

## Decision

Generate codes by hashing the long URL and Base62-encoding the digest (`0-9a-zA-Z`), keeping the first **7** characters (62^7 is about 3.5 trillion codes). The length is defined once in `api/ShortCodes.MAX_LENGTH` and shared by the request pattern, the redirect route, the `codeLength` config limit and the `VARCHAR(7)` column, which a test checks.

**Open:** hash function. Default `SHA-256`; `MD5` is the alternative. Config accepts only these two (`shortener.hashAlgorithm`).

## Consequences

- Codes are deterministic for a given input, which helps reuse (ADR-0008).
- Truncation means collisions are possible and must be handled (ADR-0007).
