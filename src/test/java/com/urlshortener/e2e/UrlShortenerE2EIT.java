package com.urlshortener.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.urlshortener.UrlShortenerApplication;
import com.urlshortener.UrlShortenerConfiguration;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests: real MySQL and Redis (Testcontainers) and the full Dropwizard app over HTTP.
 *
 * "Scaffold" tests check the wiring works today. "Target behaviour" tests describe the agreed functionality
 * and are disabled until each feature is implemented.
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
    @DisplayName("Scaffold")
    class Scaffold {

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
        void swaggerUiIsServed() {
            try (Response response = client().target(app("/docs/")).request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.readEntity(String.class)).contains("swagger-ui");
            }
        }

        @Test
        void postRejectsInvalidBody() {
            try (Response response = post(Map.of("customAlias", "bad-alias!"))) {
                assertThat(response.getStatus()).isEqualTo(422);
            }
        }

        @Test
        void postIsWiredButNotImplemented() {
            try (Response response = post(Map.of("longUrl", "https://example.com/a"))) {
                assertThat(response.getStatus()).isEqualTo(501);
            }
        }

        @Test
        void getIsWiredButNotImplemented() {
            try (Response response = get("abc1234")) {
                assertThat(response.getStatus()).isEqualTo(501);
            }
        }
    }

    @Nested
    @DisplayName("Target behaviour (enable as features land)")
    @Disabled("Business logic not implemented yet")
    class TargetBehaviour {

        @Test
        void postCreatesSevenCharBase62ShortUrl() {
            try (Response response = post(Map.of("longUrl", "https://example.com/create"))) {
                assertThat(response.getStatus()).isEqualTo(201);
                JsonNode body = response.readEntity(JsonNode.class);
                assertThat(body.path("shortCode").asText()).matches("[A-Za-z0-9]{7}");
                assertThat(body.path("shortUrl").asText()).startsWith("http://localhost/");
            }
        }

        @Test
        void getRedirectsWith302ToOriginalUrl() {
            String code = post(Map.of("longUrl", "https://example.com/redirect"))
                    .readEntity(JsonNode.class).path("shortCode").asText();
            try (Response response = get(code)) {
                assertThat(response.getStatus()).isEqualTo(302);
                assertThat(response.getHeaderString("Location")).isEqualTo("https://example.com/redirect");
            }
        }

        @Test
        void sameUrlForSameUserReturnsExistingShortCode() {
            Map<String, Object> body = Map.of("longUrl", "https://example.com/same", "userId", "user-1");
            String first = post(body).readEntity(JsonNode.class).path("shortCode").asText();
            String second = post(body).readEntity(JsonNode.class).path("shortCode").asText();
            assertThat(second).isEqualTo(first);
        }

        @Test
        void customAliasIsUsedWhenFree() {
            JsonNode body = post(Map.of("longUrl", "https://example.com/custom", "customAlias", "myLink1"))
                    .readEntity(JsonNode.class);
            assertThat(body.path("shortCode").asText()).isEqualTo("myLink1");
        }

        @Test
        void takenCustomAliasReturns409WithUpToThreeSuggestions() {
            post(Map.of("longUrl", "https://example.com/one", "customAlias", "taken01")).close();
            try (Response response = post(Map.of("longUrl", "https://example.com/two", "customAlias", "taken01"))) {
                assertThat(response.getStatus()).isEqualTo(409);
                JsonNode suggestions = response.readEntity(JsonNode.class).path("suggestions");
                assertThat(suggestions.size()).isBetween(1, 3);
            }
        }

        @Test
        void expiredShortUrlReturns410() throws InterruptedException {
            String code = post(Map.of("longUrl", "https://example.com/ttl", "ttlSeconds", 1))
                    .readEntity(JsonNode.class).path("shortCode").asText();
            Thread.sleep(1_500);
            try (Response response = get(code)) {
                assertThat(response.getStatus()).isEqualTo(410);
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
            String code = post(Map.of("longUrl", "https://example.com/fast"))
                    .readEntity(JsonNode.class).path("shortCode").asText();
            get(code).close(); // warm the cache
            long start = System.nanoTime();
            get(code).close();
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertThat(millis).isLessThan(200);
        }
    }
}
