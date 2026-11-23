package com.urlshortener.health;

import com.codahale.metrics.health.HealthCheck;
import com.urlshortener.cache.RedisClient;
import redis.clients.jedis.Jedis;

/** Reports whether Redis answers PING. Exposed on the admin port at /healthcheck. */
public class RedisHealthCheck extends HealthCheck {

    private final RedisClient redis;

    public RedisHealthCheck(RedisClient redis) {
        this.redis = redis;
    }

    @Override
    protected Result check() {
        try (Jedis jedis = redis.pool().getResource()) {
            String pong = jedis.ping();
            return "PONG".equals(pong) ? Result.healthy() : Result.unhealthy("Unexpected reply: " + pong);
        } catch (Exception e) {
            return Result.unhealthy(e);
        }
    }
}
