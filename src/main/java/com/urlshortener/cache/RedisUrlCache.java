package com.urlshortener.cache;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed {@link UrlCache}: key "url:{shortCode}", value the long URL, TTL set by the caller (never longer
 * than the short URL's remaining lifetime). Every Redis error is logged and treated as a miss, so a cache outage
 * degrades to a MySQL read instead of failing the request (ADR-0005).
 */
public class RedisUrlCache implements UrlCache {

    private static final Logger LOG = LoggerFactory.getLogger(RedisUrlCache.class);

    static final String KEY_PREFIX = "url:";

    private final RedisClient redis;
    private final Meter hits;
    private final Meter misses;
    private final Meter errors;

    public RedisUrlCache(RedisClient redis, MetricRegistry metrics) {
        this.redis = redis;
        this.hits = metrics.meter(MetricRegistry.name(RedisUrlCache.class, "hits"));
        this.misses = metrics.meter(MetricRegistry.name(RedisUrlCache.class, "misses"));
        this.errors = metrics.meter(MetricRegistry.name(RedisUrlCache.class, "errors"));
    }

    public static String key(String shortCode) {
        return KEY_PREFIX + shortCode;
    }

    @Override
    public Optional<String> get(String shortCode) {
        try (Jedis jedis = redis.pool().getResource()) {
            Optional<String> value = Optional.ofNullable(jedis.get(key(shortCode)));
            (value.isPresent() ? hits : misses).mark();
            return value;
        } catch (RuntimeException e) {
            errors.mark();
            LOG.warn("Redis get failed for {}, falling back to the database: {}", shortCode, e.toString());
            return Optional.empty();
        }
    }

    @Override
    public void put(String shortCode, String longUrl, Duration ttl) {
        long millis = ttl.toMillis();
        if (millis <= 0) {
            return;
        }
        try (Jedis jedis = redis.pool().getResource()) {
            jedis.set(key(shortCode), longUrl, SetParams.setParams().px(millis));
        } catch (RuntimeException e) {
            errors.mark();
            LOG.warn("Redis put failed for {}: {}", shortCode, e.toString());
        }
    }

    @Override
    public void evict(String shortCode) {
        try (Jedis jedis = redis.pool().getResource()) {
            jedis.del(key(shortCode));
        } catch (RuntimeException e) {
            errors.mark();
            LOG.warn("Redis evict failed for {}: {}", shortCode, e.toString());
        }
    }
}
