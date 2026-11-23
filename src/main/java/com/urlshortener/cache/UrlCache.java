package com.urlshortener.cache;

import java.time.Duration;
import java.util.Optional;

/** Read-through cache for short code to long URL lookups on the redirect path. */
public interface UrlCache {

    Optional<String> get(String shortCode);

    void put(String shortCode, String longUrl, Duration ttl);

    void evict(String shortCode);
}
