package com.urlshortener.cache;

import com.urlshortener.service.NotImplementedYetException;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed {@link UrlCache}. Planned: key "url:{shortCode}" with value longUrl and TTL equal to the
 * remaining lifetime of the short URL, capped at redis.defaultTtlSeconds.
 * {@link #get} returns empty on a miss and on any Redis error, so the service falls back to MySQL;
 * {@link #put} and {@link #evict} log and swallow errors. A cache failure never fails the redirect.
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
