# URL Shortener

A Bitly-style URL shortener built on **Dropwizard 5** (Java 21, Maven), with **MySQL** for storage and **Redis** as a cache in front of the redirect path.

> **Status: scaffold.** Every layer is wired end to end (HTTP API, service, cache, database, Swagger, health checks, integration tests), but the business logic is not written yet. `POST /api/v1/urls` and `GET /{shortCode}` currently answer **501 Not Implemented**.

## Functionality (target)

1. Convert a long URL (http or https only) into a short URL, optionally tagged with a user id. Shortening the same URL again, by any user, returns the existing short URL while it has not expired.
2. Optional custom alias (1 to 7 letters or digits). Reserved words such as `docs`, `api` or `health` are rejected with `422`. If the alias is taken, respond `409 Conflict` with up to 3 suggested alternatives.
3. Optional expiry (TTL). After expiry the short URL stops redirecting and returns `410 Gone`.
4. `GET` on a short URL redirects (`302 Found`) to the original URL.

## Non-functional goals

| Goal | How the design addresses it | Where |
|---|---|---|
| Redirect within 200ms | Redis read-through cache on `GET`; 50ms Redis socket timeout and 20ms pool max wait so a slow or exhausted cache falls back to MySQL; timers on every endpoint | `cache/`, `config.yml`, `@Timed` in `resources/` |
| High availability | Stateless app instances; MySQL and Redis health checks; pooled connections | `health/`, `database/DatabaseModule`, admin `/healthcheck` |
| Eventual consistency | Cache entries expire with the short URL's remaining lifetime; DB read replicas can be added later | `cache/RedisUrlCache` |
| Uniqueness | Hash of the long URL to Base62 (7 chars); re-hash up to 5 times on collision, then salt/counter; `UNIQUE` index on `short_code`; reuse lookup also compares the full URL so a hash collision never returns the wrong link | `service/ShortCodeGenerator`, `database/UrlMappingRepository`, `V1__create_url_mapping.sql` |
| Click analytics | 302 (not 301) so every click reaches the service; `ClickTracker` is a no-op for now | `analytics/` |

## API

| Method | Path | Success | Errors |
|---|---|---|---|
| `POST` | `/api/v1/urls` | `201` new short URL, `200` existing one reused | `422` invalid body or reserved alias (with `details`), `409` alias taken (with `suggestions`) |
| `GET` | `/{shortCode}` | `302`, `Location` = original URL | `404` unknown, `410` expired |

Request body for `POST /api/v1/urls`:

```json
{
  "longUrl": "https://example.com/some/very/long/path",
  "customAlias": "myLink1",
  "ttlSeconds": 604800,
  "userId": "user-42"
}
```

Only `longUrl` is required. Response:

```json
{
  "shortCode": "myLink1",
  "shortUrl": "http://localhost:8080/myLink1",
  "longUrl": "https://example.com/some/very/long/path",
  "expiresAt": "2026-11-30T00:00:00Z",
  "userId": "user-42"
}
```

All errors share one shape: `{"code": 409, "message": "...", "suggestions": [...]}` or `{"code": 422, "message": "Invalid request", "details": ["longUrl must be an absolute http or https URL"]}`.

**Short-code length** is defined once in `api/ShortCodes.MAX_LENGTH` (7). The request pattern, the redirect route and the `codeLength` config limit all use it, and a test checks it against the `short_code` column.

**Swagger UI:** http://localhost:8080/docs/ · **OpenAPI spec:** http://localhost:8080/openapi.json

## Project layout

```
src/main/java/com/urlshortener/
  UrlShortenerApplication.java    wires everything together
  UrlShortenerConfiguration.java  root config (bound from config.yml)
  config/      RedisConfig, ShortenerConfig
  api/         request/response DTOs
  resources/   UrlResource (POST), RedirectResource (GET)
  service/     conversion: UrlShortenerService, ShortCodeGenerator, Base62, AliasSuggester, exceptions
  database/    DatabaseModule (connection pool + Flyway), UrlMappingRepository (JDBI), UrlMapping
  cache/       UrlCache, RedisUrlCache, RedisClient
  analytics/   ClickTracker
  health/      RedisHealthCheck (MySQL health check is built in)
  errors/      exception mappers for 404 / 409 / 410 / 422 / 501
  validation/  @HttpUrl (http/https only)
src/main/resources/
  db/migration/V1__create_url_mapping.sql
  swagger/index.html
src/test/java/com/urlshortener/
  e2e/UrlShortenerE2EIT.java      end-to-end tests over HTTP
  e2e/RedisDownE2EIT.java         app behaviour with Redis unavailable
  support/Infrastructure.java     Testcontainers MySQL + Redis
```

## Running locally

Prerequisites: Java 21, Maven 3.9+, Docker.

```bash
docker compose up -d                         # MySQL on 3306, Redis on 6379
mvn package -DskipTests
java -jar target/url-shortener-0.1.0-SNAPSHOT.jar server config.yml
```

- App: http://localhost:8080 · Swagger: http://localhost:8080/docs/
- Admin (health, metrics): http://localhost:8081/healthcheck, http://localhost:8081/metrics

Settings can be overridden with environment variables: `PORT`, `ADMIN_PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `BASE_URL`.

## Tests

```bash
mvn test      # unit tests
mvn verify    # + end-to-end integration tests (needs Docker; starts MySQL and Redis via Testcontainers)
```

The end-to-end suite has two groups:

- **Scaffold** (enabled): app boots, migration runs, column length matches `ShortCodes`, MySQL and Redis are healthy, OpenAPI lists both endpoints with 200/201/409/422, Swagger UI is served, invalid input, non-http URLs and reserved aliases get 422 in the `ErrorResponse` shape, both endpoints answer 501.
- **Redis down** (`RedisDownE2EIT`): the app starts with no Redis, reports it unhealthy in under 2 seconds and keeps serving requests.
- **Target behaviour** (disabled): 201 on create, 200 on reuse, same URL from different users gives the same code, 302 redirect, custom alias, 409 with suggestions, 410 after expiry, 404 for unknown codes, redirect under 200ms, redirect with Redis down. Enable each test as its feature is implemented.

Unit tests cover URL validation, reserved aliases and config limits. `ShortCodeGeneratorTest` (Base62 and the collision policy) is disabled until that logic exists.

## Known trade-offs

- **Concurrent duplicates.** Two simultaneous requests for the same new URL can both insert, giving two codes for one URL. Both work; the reuse lookup returns the oldest row, so later requests converge on one code. Accepted under eventual consistency.

## Design decisions still open

These defaults are in place and each lives in a single config value or class:

1. Hash function: **SHA-256** (alternative: MD5), `shortener.hashAlgorithm`.
2. Custom alias length: **1 to 7** Base62 characters (alternative: exactly 7), `ShortenRequest.customAlias`.
3. TTL format: **seconds** in `ttlSeconds` (alternative: ISO-8601 duration), no default or maximum.
4. Click counting: **no-op** `ClickTracker` for now.
