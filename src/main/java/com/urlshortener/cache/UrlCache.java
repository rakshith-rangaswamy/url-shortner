package com.urlshortener.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-aside store for short code to long URL lookups on the redirect path. MySQL stays the source of
 * truth: on a miss the service reads the database and calls {@link #put} to refill the cache.
 * Implementations must never throw; an unavailable cache behaves as a miss.
 */
public interface UrlCache {

    Optional<String> get(String shortCode);

    void put(String shortCode, String longUrl, Duration ttl);

    void evict(String shortCode);
}
