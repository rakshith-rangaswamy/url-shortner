# ADR-0005: Redis cache-aside in front of the redirect, falling back to MySQL

- **Status:** Accepted
- **Date:** 2026-11-22
- **Deciders:** Rakshith

## Context

Redirects must respond within **200 ms**. The redirect path is read-heavy, and a database round trip on every click would make that target fragile under load. The cache must never become a hard dependency.

## Decision

Put **Redis 7** in front of `GET /{shortCode}` using **cache-aside**, with MySQL as the source of truth:

1. Read `url:{shortCode}` from Redis; a hit redirects immediately.
2. On a miss, or any Redis error or timeout, read MySQL.
3. Unknown code: 404. Expired: 410, not cached.
4. Valid: write to Redis with TTL = min(remaining lifetime, `redis.defaultTtlSeconds`), then redirect.

Redis calls are bounded: 50 ms socket timeout and 20 ms pool max wait (`redis.timeoutMillis`, `redis.maxWaitMillis`). `UrlCache` implementations never throw; errors behave as a miss. Redis health is reported on the admin port but does not stop the app from serving.

## Consequences

- A cached entry can never outlive its short URL, so cache hits need no expiry check.
- With Redis down, redirects still work from MySQL (covered by `RedisDownE2EIT`).
- Implemented in `UrlShortenerService.resolve` and `RedisUrlCache`; cache hits, misses and errors are counted in Dropwizard Metrics.
