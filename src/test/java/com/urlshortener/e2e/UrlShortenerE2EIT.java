package com.urlshortener.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.urlshortener.UrlShortenerApplication;
import com.urlshortener.UrlShortenerConfiguration;
import com.urlshortener.api.ShortCodes;
import com.urlshortener.support.Infrastructure;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jdbi.v3.core.Jdbi;
import redis.clients.jedis.Jedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests: real MySQL and Redis (Testcontainers) and the full Dropwizard app over HTTP.
 *
 * "Platform" covers schema, health, OpenAPI and validation. "Shorten" and "Redirect" cover the API behaviour,
 * including the Redis cache-aside path.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class UrlShortenerE2EIT {

    static final DropwizardAppExtension<UrlShortenerConfiguration> APP = new DropwizardAppExtension<>(
            UrlShortenerApplication.class,
            ResourceHelpers.resourceFilePath("test-config.yml"),
            ConfigOverride.config("database.url", Infrastructure.MYSQL::getJdbcUrl),
            ConfigOverride.config("database.user", Infrastructure.MYSQL::getUsername),
            ConfigOverride.config("database.password", Infrastructure.MYSQL::getPassword),
            ConfigOverride.config("redis.host", Infrastructure.REDIS::getHost),
            ConfigOverride.config("redis.port", () -> String.valueOf(Infrastructure.REDIS.getMappedPort(6379))));

    private static String app(String path) {
        return "http://localhost:" + APP.getLocalPort() + path;
    }

    private static String admin(String path) {
        return "http://localhost:" + APP.getAdminPort() + path;
    }

    private static Client client() {
        return APP.client();
    }

    private static Response post(Map<String, Object> body) {
        return client().target(app("/api/v1/urls")).request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));
    }

    private static Response get(String shortCode) {
        return client().target(app("/" + shortCode)).request()
                .property("jersey.config.client.followRedirects", false)
                .get();
    }

    @Nested
    @DisplayName("Platform: schema, health, docs, validation")
    class Platform {

        @Test
        void migrationCreatesUrlMappingTable() {
            Jdbi jdbi = Jdbi.create(Infrastructure.MYSQL.getJdbcUrl(),
                    Infrastructure.MYSQL.getUsername(), Infrastructure.MYSQL.getPassword());
            int tables = jdbi.withHandle(h -> h.createQuery(
                            "SELECT COUNT(*) FROM information_schema.tables "
                                    + "WHERE table_schema = DATABASE() AND table_name = 'url_mapping'")
                    .mapTo(Integer.class).one());
            assertThat(tables).isEqualTo(1);
        }

        @Test
        void shortCodeColumnLengthMatchesShortCodesMaxLength() {
            Jdbi jdbi = Jdbi.create(Infrastructure.MYSQL.getJdbcUrl(),
                    Infrastructure.MYSQL.getUsername(), Infrastructure.MYSQL.getPassword());
            int length = jdbi.withHandle(h -> h.createQuery(
                            "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns "
                                    + "WHERE table_schema = DATABASE() AND table_name = 'url_mapping' "
                                    + "AND column_name = 'short_code'")
                    .mapTo(Integer.class).one());
            assertThat(length).isEqualTo(ShortCodes.MAX_LENGTH);
        }

        @Test
        void healthChecksReportMysqlAndRedisHealthy() {
            JsonNode health = client().target(admin("/healthcheck")).request().get(JsonNode.class);
            assertThat(health.path("mysql").path("healthy").asBoolean()).isTrue();
            assertThat(health.path("redis").path("healthy").asBoolean()).isTrue();
        }

        @Test
        void openApiDescribesPostAndGetEndpoints() {
            JsonNode spec = client().target(app("/openapi.json")).request().get(JsonNode.class);
            assertThat(spec.path("paths").path("/api/v1/urls").has("post")).isTrue();
            assertThat(spec.path("paths").path("/{shortCode}").has("get")).isTrue();
        }

        @Test
        void openApiDocumentsBoth200And201ForPost() {
            JsonNode responses = client().target(app("/openapi.json")).request().get(JsonNode.class)
                    .path("paths").path("/api/v1/urls").path("post").path("responses");
            assertThat(responses.has("200")).isTrue();
            assertThat(responses.has("201")).isTrue();
            assertThat(responses.has("409")).isTrue();
            assertThat(responses.has("422")).isTrue();
        }

        @Test
        void swaggerUiIsServed() {
            try (Response response = client().target(app("/docs/")).request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.readEntity(String.class)).contains("swagger-ui");
            }
        }

        @Test
        void postRejectsInvalidBodyWithErrorResponseShape() {
            try (Response response = post(Map.of("customAlias", "bad-alias!"))) {
                assertThat(response.getStatus()).isEqualTo(422);
                JsonNode body = response.readEntity(JsonNode.class);
                assertThat(body.path("code").asInt()).isEqualTo(422);
                assertThat(body.path("message").asText()).isEqualTo("Invalid request");
                assertThat(body.path("details").toString()).contains("longUrl").contains("customAlias");
            }
        }

        @Test
        void postRejectsNonHttpUrls() {
            for (String url : new String[]{"javascript:alert(1)", "ftp://example.com/x", "example.com"}) {
                try (Response response = post(Map.of("longUrl", url))) {
                    assertThat(response.getStatus()).as(url).isEqualTo(422);
                }
            }
        }

        @Test
        void postRejectsAliasShorterThanThreeAndTtlOutOfRange() {
            try (Response r = post(Map.of("longUrl", "https://example.com/v", "customAlias", "ab"))) {
                assertThat(r.getStatus()).isEqualTo(422);
            }
            try (Response r = post(Map.of("longUrl", "https://example.com/v", "ttlSeconds", 59))) {
                assertThat(r.getStatus()).isEqualTo(422);
            }
            try (Response r = post(Map.of("longUrl", "https://example.com/v", "ttlSeconds", 315_360_001L))) {
                assertThat(r.getStatus()).isEqualTo(422);
            }
        }

        @Test
        void postRejectsReservedAlias() {
            for (String alias : new String[]{"docs", "Docs", "api", "health"}) {
                try (Response response = post(Map.of("longUrl", "https://example.com/r", "customAlias", alias))) {
                    assertThat(response.getStatus()).as(alias).isEqualTo(422);
                    assertThat(response.readEntity(JsonNode.class).path("details").toString()).contains("reserved");
                }
            }
        }
    }

    /** A URL no other test uses, so tests stay independent in the shared database. */
    private static String uniqueUrl(String label) {
        return "https://example.com/" + label + "/" + UUID.randomUUID();
    }

    private static JsonNode create(Map<String, Object> body) {
        try (Response response = post(body)) {
            assertThat(response.getStatus()).isIn(200, 201);
            return response.readEntity(JsonNode.class);
        }
    }

    /** Moves a mapping's expiry into the past and drops it from Redis, as if its TTL had run out. */
    private static void expireNow(String shortCode) {
        Infrastructure.jdbi().useHandle(h -> h.createUpdate(
                        "UPDATE url_mapping SET expires_at = :past WHERE short_code = :code")
                .bind("past", Timestamp.from(Instant.now().minusSeconds(1)))
                .bind("code", shortCode)
                .execute());
        try (Jedis redis = Infrastructure.redis()) {
            redis.del("url:" + shortCode);
        }
    }

    @Nested
    @DisplayName("Shorten")
    class Shorten {

        @Test
        void createsSevenCharBase62ShortUrlWith201AndLocation() {
            String url = uniqueUrl("create");
            try (Response response = post(Map.of("longUrl", url))) {
                assertThat(response.getStatus()).isEqualTo(201);
                JsonNode body = response.readEntity(JsonNode.class);
                String code = body.path("shortCode").asText();
                assertThat(code).matches("[A-Za-z0-9]{7}");
                assertThat(body.path("shortUrl").asText()).isEqualTo("http://localhost/" + code);
                assertThat(body.path("longUrl").asText()).isEqualTo(url);
                assertThat(response.getHeaderString("Location")).isEqualTo("http://localhost/" + code);
            }
        }

        @Test
        void sameUrlReturnsExistingShortCodeWith200() {
            Map<String, Object> body = Map.of("longUrl", uniqueUrl("same"));
            String first;
            try (Response created = post(body)) {
                assertThat(created.getStatus()).isEqualTo(201);
                first = created.readEntity(JsonNode.class).path("shortCode").asText();
            }
            try (Response reused = post(body)) {
                assertThat(reused.getStatus()).isEqualTo(200);
                assertThat(reused.readEntity(JsonNode.class).path("shortCode").asText()).isEqualTo(first);
            }
        }

        @Test
        void sameUrlFromDifferentUsersReturnsSameShortCode() {
            String url = uniqueUrl("shared");
            String a = create(Map.of("longUrl", url, "userId", "user-a")).path("shortCode").asText();
            JsonNode b = create(Map.of("longUrl", url, "userId", "user-b", "ttlSeconds", 600));
            assertThat(b.path("shortCode").asText()).isEqualTo(a);
            assertThat(b.path("userId").asText()).isEqualTo("user-a");
            assertThat(b.has("expiresAt")).isFalse();
        }

        @Test
        void ttlSetsExpiresAt() {
            JsonNode body = create(Map.of("longUrl", uniqueUrl("ttl"), "ttlSeconds", 3600));
            Instant expiresAt = Instant.parse(body.path("expiresAt").asText());
            assertThat(expiresAt).isBetween(Instant.now().plusSeconds(3500), Instant.now().plusSeconds(3700));
        }

        @Test
        void customAliasIsUsedWhenFree() {
            String alias = "c" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            try (Response response = post(Map.of("longUrl", uniqueUrl("custom"), "customAlias", alias))) {
                assertThat(response.getStatus()).isEqualTo(201);
                assertThat(response.readEntity(JsonNode.class).path("shortCode").asText()).isEqualTo(alias);
            }
        }

        @Test
        void takenCustomAliasReturns409WithThreeFreeSuggestions() {
            post(Map.of("longUrl", uniqueUrl("one"), "customAlias", "taken")).close();
            try (Response response = post(Map.of("longUrl", uniqueUrl("two"), "customAlias", "taken"))) {
                assertThat(response.getStatus()).isEqualTo(409);
                JsonNode body = response.readEntity(JsonNode.class);
                assertThat(body.path("code").asInt()).isEqualTo(409);
                JsonNode suggestions = body.path("suggestions");
                assertThat(suggestions.size()).isEqualTo(3);
                for (JsonNode s : suggestions) {
                    try (Response free = post(Map.of("longUrl", uniqueUrl("alt"), "customAlias", s.asText()))) {
                        assertThat(free.getStatus()).as(s.asText()).isEqualTo(201);
                    }
                }
            }
        }

        @Test
        void newShortUrlIsCachedImmediately() {
            String code = create(Map.of("longUrl", uniqueUrl("warm"))).path("shortCode").asText();
            try (Jedis redis = Infrastructure.redis()) {
                assertThat(redis.get("url:" + code)).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Redirect (Redis first, MySQL on a miss)")
    class Redirect {

        @Test
        void redirectsWith302ToOriginalUrl() {
            String url = uniqueUrl("redirect");
            String code = create(Map.of("longUrl", url)).path("shortCode").asText();
            try (Response response = get(code)) {
                assertThat(response.getStatus()).isEqualTo(302);
                assertThat(response.getHeaderString("Location")).isEqualTo(url);
            }
        }

        @Test
        void cacheMissReadsMysqlAndRefillsCache() {
            String url = uniqueUrl("miss");
            String code = create(Map.of("longUrl", url)).path("shortCode").asText();
            try (Jedis redis = Infrastructure.redis()) {
                redis.del("url:" + code);
                try (Response response = get(code)) {
                    assertThat(response.getStatus()).isEqualTo(302);
                    assertThat(response.getHeaderString("Location")).isEqualTo(url);
                }
                assertThat(redis.get("url:" + code)).isEqualTo(url);
                assertThat(redis.pttl("url:" + code)).isPositive();
            }
        }

        @Test
        void cacheEntryDoesNotOutliveTheShortUrl() {
            String code = create(Map.of("longUrl", uniqueUrl("soon"), "ttlSeconds", 60)).path("shortCode").asText();
            try (Jedis redis = Infrastructure.redis()) {
                redis.del("url:" + code);
                get(code).close();
                assertThat(redis.pttl("url:" + code)).isBetween(1L, 60_000L);
            }
        }

        @Test
        void expiredShortUrlReturns410AndIsNotCached() {
            String code = create(Map.of("longUrl", uniqueUrl("gone"), "ttlSeconds", 60)).path("shortCode").asText();
            expireNow(code);
            try (Response response = get(code)) {
                assertThat(response.getStatus()).isEqualTo(410);
                assertThat(response.readEntity(JsonNode.class).path("code").asInt()).isEqualTo(410);
            }
            try (Jedis redis = Infrastructure.redis()) {
                assertThat(redis.exists("url:" + code)).isFalse();
            }
        }

        @Test
        void sameUrlAfterExpiryGetsANewCode() {
            String url = uniqueUrl("again");
            String first = create(Map.of("longUrl", url, "ttlSeconds", 60)).path("shortCode").asText();
            expireNow(first);
            try (Response response = post(Map.of("longUrl", url))) {
                assertThat(response.getStatus()).isEqualTo(201);
                assertThat(response.readEntity(JsonNode.class).path("shortCode").asText()).isNotEqualTo(first);
            }
        }

        @Test
        void unknownShortCodeReturns404() {
            try (Response response = get("zzzzzzz")) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        @Test
        void redirectRespondsWithin200ms() {
            String code = create(Map.of("longUrl", uniqueUrl("fast"))).path("shortCode").asText();
            get(code).close(); // warm up
            long start = System.nanoTime();
            try (Response response = get(code)) {
                long millis = (System.nanoTime() - start) / 1_000_000;
                assertThat(response.getStatus()).isEqualTo(302);
                assertThat(millis).isLessThan(200);
            }
        }
    }
}
