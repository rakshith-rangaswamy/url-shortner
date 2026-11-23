# Architecture Decision Records

Decisions for the URL shortener, one file each. See [ADR-0001](0001-record-architecture-decisions.md) for how this works.

**Adding a decision:** copy [`0000-template.md`](0000-template.md) to the next number, fill it in, and add a row below. To change a decision, write a new ADR and mark the old one *Superseded by ADR-NNNN*.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-dropwizard-over-spring-boot.md) | Use Dropwizard instead of Spring Boot | Accepted |
| [0003](0003-java-21-and-maven.md) | Java 21 and Maven | Accepted |
| [0004](0004-mysql-jdbi-flyway.md) | MySQL as the source of truth, accessed with JDBI and Flyway | Accepted |
| [0005](0005-redis-cache-aside-with-mysql-fallback.md) | Redis cache-aside in front of the redirect, falling back to MySQL | Accepted |
| [0006](0006-hash-base62-seven-char-codes.md) | Short codes are a hash of the long URL, Base62-encoded, 7 characters | Accepted (hash function Proposed) |
| [0007](0007-collision-rehash-then-salt.md) | Collision handling: re-hash up to 5 times, then salt or counter | Accepted |
| [0008](0008-reuse-short-url-for-same-long-url.md) | Reuse the existing short URL for the same long URL, for any user | Accepted |
| [0009](0009-custom-alias-rules.md) | Custom aliases: Base62 only, reserved words rejected, 409 with suggestions when taken | Accepted (length rule Proposed) |
| [0010](0010-ttl-expiry-and-410-gone.md) | Expiry is given as a TTL; expired links return 410 Gone | Accepted (TTL format Proposed) |
| [0011](0011-redirect-with-302.md) | Redirect with 302 Found | Accepted |
| [0012](0012-optional-user-id-no-auth.md) | Optional userId, no authentication yet | Accepted |
| [0013](0013-http-https-long-urls-only.md) | Accept only absolute http and https long URLs | Accepted |
| [0014](0014-single-error-response-shape.md) | One JSON error shape for every error status | Accepted |
| [0015](0015-testcontainers-e2e-and-disabled-target-tests.md) | End-to-end tests with Testcontainers; target behaviour kept as disabled tests | Accepted |
| [0016](0016-availability-and-eventual-consistency.md) | Stateless instances, health checks and eventual consistency for availability | Accepted |
| [0017](0017-scaffold-first-501.md) | Scaffold first: unimplemented behaviour returns 501 | Accepted |
| [0018](0018-click-analytics-no-op.md) | Click analytics: no-op tracker for now | Proposed |

## Open questions

These are recorded as *Proposed* with the default currently in the code:

- Hash function: SHA-256 or MD5 (ADR-0006)
- Custom alias length: exactly 7 or 1 to 7 (ADR-0009)
- TTL format, default and maximum (ADR-0010)
- Click counting scope and storage (ADR-0018)
