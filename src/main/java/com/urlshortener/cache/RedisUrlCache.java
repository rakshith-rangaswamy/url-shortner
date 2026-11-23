package com.urlshortener.cache;

import com.urlshortener.service.NotImplementedYetException;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed {@link UrlCache}. Planned: key "url:{shortCode}" with value longUrl and TTL equal to the
 * remaining lifetime of the short URL (or the configured default). Cache errors must degrade to a DB read,
 * never fail the redirect.
 */
public class RedisUrlCache implements UrlCache {

    static final String KEY_PREFIX = "url:";

    private final RedisClient redis;

    public RedisUrlCache(RedisClient redis) {
        this.redis = redis;
    }

    @Override
    public Optional<String> get(String shortCode) {
        throw new NotImplementedYetException("RedisUrlCache.get");
    }

    @Override
    public void put(String shortCode, String longUrl, Duration ttl) {
        throw new NotImplementedYetException("RedisUrlCache.put");
    }

    @Override
    public void evict(String shortCode) {
        throw new NotImplementedYetException("RedisUrlCache.evict");
    }
}
