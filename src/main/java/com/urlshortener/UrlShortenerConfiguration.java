package com.urlshortener;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.urlshortener.config.RedisConfig;
import com.urlshortener.config.ShortenerConfig;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Root configuration, bound from config.yml. */
public class UrlShortenerConfiguration extends Configuration {

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    private RedisConfig redis = new RedisConfig();

    @Valid
    @NotNull
    private ShortenerConfig shortener = new ShortenerConfig();

    @JsonProperty("database")
    public DataSourceFactory getDatabase() { return database; }

    @JsonProperty("database")
    public void setDatabase(DataSourceFactory database) { this.database = database; }

    @JsonProperty("redis")
    public RedisConfig getRedis() { return redis; }

    @JsonProperty("redis")
    public void setRedis(RedisConfig redis) { this.redis = redis; }

    @JsonProperty("shortener")
    public ShortenerConfig getShortener() { return shortener; }

    @JsonProperty("shortener")
    public void setShortener(ShortenerConfig shortener) { this.shortener = shortener; }
}
