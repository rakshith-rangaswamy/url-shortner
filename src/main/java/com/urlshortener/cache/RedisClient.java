package com.urlshortener.cache;

import com.urlshortener.config.RedisConfig;
import io.dropwizard.lifecycle.Managed;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/** Owns the Redis connection pool; started and stopped with the application. */
public class RedisClient implements Managed {

    private final RedisConfig config;
    private volatile JedisPool pool;

    public RedisClient(RedisConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.getMaxTotalConnections());
        poolConfig.setMaxWait(Duration.ofMillis(config.getMaxWaitMillis()));
        poolConfig.setBlockWhenExhausted(true);
        pool = new JedisPool(poolConfig, config.getHost(), config.getPort(), config.getTimeoutMillis());
    }

    @Override
    public void stop() {
        if (pool != null) {
            pool.close();
        }
    }

    public JedisPool pool() {
        if (pool == null) {
            throw new IllegalStateException("Redis pool not started");
        }
        return pool;
    }
}
