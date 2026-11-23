package com.urlshortener.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.urlshortener.UrlShortenerApplication;
import com.urlshortener.UrlShortenerConfiguration;
import com.urlshortener.support.Infrastructure;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The app pointed at a Redis port where nothing listens. It must still start, report Redis unhealthy
 * quickly (no hang), and keep serving: the cache is an optimisation, not a dependency.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class RedisDownE2EIT {

    private static final int DEAD_REDIS_PORT = freePort();

    static final DropwizardAppExtension<UrlShortenerConfiguration> APP = new DropwizardAppExtension<>(
            UrlShortenerApplication.class,
            ResourceHelpers.resourceFilePath("test-config.yml"),
            ConfigOverride.config("database.url", Infrastructure.MYSQL::getJdbcUrl),
            ConfigOverride.config("database.user", Infrastructure.MYSQL::getUsername),
            ConfigOverride.config("database.password", Infrastructure.MYSQL::getPassword),
            ConfigOverride.config("redis.host", "localhost"),
            ConfigOverride.config("redis.port", String.valueOf(DEAD_REDIS_PORT)));

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void healthCheckReportsRedisDownWithoutHanging() {
        long start = System.nanoTime();
        try (Response response = APP.client()
                .target("http://localhost:" + APP.getAdminPort() + "/healthcheck").request().get()) {
            long millis = (System.nanoTime() - start) / 1_000_000;
            JsonNode health = response.readEntity(JsonNode.class);
            assertThat(health.path("redis").path("healthy").asBoolean()).isFalse();
            assertThat(health.path("mysql").path("healthy").asBoolean()).isTrue();
            assertThat(millis).isLessThan(2_000);
        }
    }

    @Test
    void apiStillServesRequests() {
        try (Response response = APP.client()
                .target("http://localhost:" + APP.getLocalPort() + "/api/v1/urls")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(Map.of("longUrl", "https://example.com/no-cache")))) {
            assertThat(response.getStatus()).isEqualTo(501); // wired, not implemented yet
        }
    }

    @Test
    @Disabled("Business logic not implemented yet")
    void redirectFallsBackToMysqlWithin200ms() {
        // create a short URL, then GET it: expect 302 to the long URL in under 200ms with Redis down
    }
}
