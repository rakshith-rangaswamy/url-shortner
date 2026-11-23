# ADR-0006: Short codes are a hash of the long URL, Base62-encoded, 7 characters

- **Status:** Accepted
- **Date:** 2026-11-22 (hash function decided 2026-11-23)
- **Deciders:** Rakshith

## Context

Each short URL needs a unique, compact code. A global counter would need coordination across instances; the owner chose hashing to avoid that complexity.

## Decision

Generate codes by hashing the owner and long URL (`userId + "|" + longUrl`, since ADR-0020) and Base62-encoding the digest (`0-9a-zA-Z`), keeping the **7** characters (62^7 is about 3.5 trillion codes). The length is defined once in `api/ShortCodes.MAX_LENGTH` and shared by the request pattern, the redirect route, the `codeLength` config limit and the `VARCHAR(7)` column, which a test checks.

**Hash function: SHA-256** (decided 2026-11-23). MD5 was rejected because MD5 collisions can be crafted on purpose, which would let someone make two URLs share a code; SHA-256 costs microseconds more, which is negligible next to a database write. `shortener.hashAlgorithm` still accepts `MD5` for experiments, nothing else.

The code is the hash modulo 62^7, written as 7 Base62 digits (`Base62.encodeFixed`). Taking the low-order digits keeps every character uniformly distributed.

## Consequences

- Codes are deterministic for a given input, which helps reuse (ADR-0008).
- Truncation means collisions are possible and must be handled (ADR-0007).
