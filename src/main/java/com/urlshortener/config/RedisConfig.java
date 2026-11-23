package com.urlshortener.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Connection settings for the Redis cache that sits in front of the redirect path. */
public class RedisConfig {

    @NotBlank
    @JsonProperty
    private String host = "localhost";

    @Min(1)
    @JsonProperty
    private int port = 6379;

    /** Socket timeout. Kept small so a slow cache cannot blow the 200ms redirect budget. */
    @Min(1)
    @JsonProperty
    private int timeoutMillis = 50;

    @Min(1)
    @JsonProperty
    private int maxTotalConnections = 64;

    /**
     * Longest a request waits for a free pooled connection. Without it Jedis waits forever when the pool is
     * exhausted, which would stall redirects instead of letting them fall back to MySQL.
     */
    @Min(1)
    @JsonProperty
    private int maxWaitMillis = 20;

    /** Cache TTL used when a short URL has no expiry of its own. */
    @Min(1)
    @JsonProperty
    private long defaultTtlSeconds = 86_400;

    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getTimeoutMillis() { return timeoutMillis; }
    public int getMaxTotalConnections() { return maxTotalConnections; }
    public int getMaxWaitMillis() { return maxWaitMillis; }
    public long getDefaultTtlSeconds() { return defaultTtlSeconds; }
}
