# URL Shortener

A Bitly-style URL shortener built on **Dropwizard 5** (Java 21, Maven), with **MySQL** as the source of truth and **Redis** as a cache in front of the redirect path.

## Functionality

1. **Shorten** an http or https URL into a 7-character Base62 code, optionally tagged with a `userId`. Shortening the same URL again, by any user, returns the existing short URL (`200`) while it has not expired; otherwise a new one is created (`201`).
2. **Custom alias** (optional): 3 to 7 letters or digits. Reserved words such as `docs`, `api` or `health` are rejected (`422`). A taken alias returns `409 Conflict` with up to 3 free suggestions.
3. **Expiry** (optional): `ttlSeconds` from 60 seconds to 10 years. After expiry the short URL returns `410 Gone`. Expired codes are never handed out again.
4. **Redirect**: `GET /{shortCode}` answers `302 Found` with the original URL.

## API

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/api/v1/urls` | `201` new short URL (with `Location`), `200` existing one reused | `422` invalid body or reserved alias, `409` alias taken (with `suggestions`), `503` no free code (practically unreachable) |
| `GET` | `/{shortCode}` | `302`, `Location` = original URL | `404` unknown, `410` expired |

```bash
curl -i -X POST localhost:8080/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"longUrl": "https://example.com/some/very/long/path", "customAlias": "promo", "ttlSeconds": 604800, "userId": "user-42"}'
```

Only `longUrl` is required. Response (`expiresAt` is omitted when the link never expires):

```json
{
  "shortCode": "promo",
  "shortUrl": "http://localhost:8080/promo",
  "longUrl": "https://example.com/some/very/long/path",
  "expiresAt": "2026-11-30T00:00:00Z",
  "userId": "user-42"
}
```

A taken alias:

```json
{ "code": 409, "message": "Custom alias 'promo' is already taken", "suggestions": ["promo1", "promo2", "promo3"] }
```

All errors share this shape; validation errors carry `details`, one entry per invalid field.

When an existing short URL is reused, the new request's `ttlSeconds` and `userId` are ignored and the stored values are returned.

**Swagger UI:** http://localhost:8080/docs/ · **OpenAPI spec:** http://localhost:8080/openapi.json

## How it works

**Short codes** (ADR-0006, ADR-0007). The code is SHA-256 of the URL, reduced to 7 Base62 characters (62^7 is about 3.5 trillion codes). On a collision with a different URL the hash is re-hashed up to 5 times, then a counter is appended (`url#1`, `url#2`, ...). Up to 20 candidates are tried.

**Uniqueness** (ADR-0019). The `UNIQUE` index on `short_code` is the check: the service inserts the row, and a duplicate-key error means the code is taken. Two concurrent requests for the same new URL converge on one code.

**Redirect read path** (ADR-0005), cache-aside with MySQL as the source of truth:

1. Look up `url:{shortCode}` in Redis. A hit redirects immediately.
2. On a miss, or if Redis errors or times out, read the row from MySQL.
3. No row: `404`. Expired row: `410`, and nothing is cached.
4. Valid row: write it to Redis with TTL = min(remaining lifetime, `redis.defaultTtlSeconds`), so a cached entry can never outlive its short URL. Then redirect.

New short URLs are written to Redis as soon as they are created.

## Non-functional goals

| Goal | How | Where |
|---|---|---|
| Redirect within 200ms | Redis cache-aside on `GET`; 50ms Redis socket timeout and 20ms pool max wait so a slow cache falls back to MySQL; request timers and cache hit/miss/error meters | `cache/`, `config.yml`, admin `/metrics` |
| High availability | Stateless app instances; MySQL and Redis health checks; the app keeps serving from MySQL when Redis is down | `health/`, admin `/healthcheck` |
| Eventual consistency | Cache entries expire with the short URL; reuse lookups read MySQL | `cache/RedisUrlCache`, `service/` |
| Uniqueness | Hash + Base62, re-hash then salt, unique index as the final check | `service/ShortCodeGenerator`, `V1__create_url_mapping.sql` |
| Click analytics | `302` so every click reaches the service; counting itself is out of scope (`ClickTracker` is a no-op) | `analytics/` |

## Project layout

```
src/main/java/com/urlshortener/
  UrlShortenerApplication.java    wires everything together
  UrlShortenerConfiguration.java  root config (bound from config.yml)
  config/      RedisConfig, ShortenerConfig
  api/         request/response DTOs, ShortCodes (length and patterns)
  resources/   UrlResource (POST), RedirectResource (GET)
  service/     UrlShortenerService, ShortCodeGenerator, Base62, AliasSuggester, AliasPolicy, exceptions
  database/    DatabaseModule (pool + Flyway), UrlMappingRepository (JDBI), UrlMapping, DuplicateKeys
  cache/       UrlCache, RedisUrlCache, RedisClient
  analytics/   ClickTracker
  health/      RedisHealthCheck (MySQL health check is built in)
  errors/      exception mappers for 404 / 409 / 410 / 422 / 503
  validation/  @HttpUrl (http/https only)
src/main/resources/
  db/migration/V1__create_url_mapping.sql
  swagger/index.html
src/test/java/com/urlshortener/
  service/, config/, validation/  unit tests
  e2e/UrlShortenerE2EIT.java      end-to-end tests over HTTP
  e2e/RedisDownE2EIT.java         behaviour with Redis unavailable
  support/Infrastructure.java     Testcontainers MySQL + Redis
docs/adr/                         architecture decision records
```

## Running locally

Prerequisites: Java 21, Maven 3.9+, Docker.

```bash
docker compose up -d --wait                  # MySQL on 3306, Redis on 6379
mvn package -DskipTests
java -jar target/url-shortener-0.1.0-SNAPSHOT.jar server config.yml
```

- App: http://localhost:8080 · Swagger: http://localhost:8080/docs/
- Admin (health, metrics): http://localhost:8081/healthcheck, http://localhost:8081/metrics

On the very first `docker compose up`, MySQL restarts once while it initialises; if the app reports "Communications link failure", start it again a few seconds later.

Settings can be overridden with environment variables: `PORT`, `ADMIN_PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `BASE_URL`.

## Tests

```bash
mvn test      # unit tests
mvn verify    # + end-to-end tests (needs Docker; starts MySQL and Redis via Testcontainers)
```

- **Unit:** Base62, code generation and the collision policy, alias suggestions, reserved aliases, URL validation, config limits, and the service (create, reuse, collisions, salt fallback, races, custom aliases, 409, cache-aside redirect, 404, 410, cache TTL capping) against in-memory fakes.
- **End-to-end:** schema and health, OpenAPI, validation errors, create (201), reuse (200, any user), TTL, custom alias, 409 with 3 usable suggestions, 302 redirect, cache refill after a miss, cache TTL capped by expiry, 410 after expiry, new code after expiry, 404, redirect under 200ms.
- **Redis down:** the app starts without Redis, reports it unhealthy in under 2 seconds, and still shortens and redirects from MySQL within 200ms.

## Architecture decisions

All decisions, with context and trade-offs, are recorded in [`docs/adr/`](docs/adr/README.md).
