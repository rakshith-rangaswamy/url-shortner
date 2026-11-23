package com.urlshortener.service;

import com.urlshortener.analytics.ClickTracker;
import com.urlshortener.api.ShortenRequest;
import com.urlshortener.cache.UrlCache;
import com.urlshortener.database.UrlMappingRepository;

import java.net.URI;

/**
 * Conversion service: long URL to short URL, and short code back to the long URL.
 *
 * All collaborators are wired; the business logic is intentionally left unimplemented.
 */
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final UrlCache cache;
    private final ShortCodeGenerator codeGenerator;
    private final AliasSuggester aliasSuggester;
    private final AliasPolicy aliasPolicy;
    private final ClickTracker clickTracker;

    public UrlShortenerService(UrlMappingRepository repository,
                               UrlCache cache,
                               ShortCodeGenerator codeGenerator,
                               AliasSuggester aliasSuggester,
                               AliasPolicy aliasPolicy,
                               ClickTracker clickTracker) {
        this.repository = repository;
        this.cache = cache;
        this.codeGenerator = codeGenerator;
        this.aliasSuggester = aliasSuggester;
        this.aliasPolicy = aliasPolicy;
        this.clickTracker = clickTracker;
    }

    /**
     * Planned flow:
     *  - customAlias given: reserved words are rejected (done, see {@link AliasPolicy}); if taken, throw
     *    {@link AliasTakenException} with suggestions, else store it.
     *  - otherwise: if an unexpired generated mapping exists for the same long URL (any user), return it with
     *    {@code created=false}; else generate a code via {@link ShortCodeGenerator} with the collision policy and
     *    store it with {@code created=true}.
     *  - concurrent requests for the same new URL may both insert (two codes for one URL). Accepted under
     *    eventual consistency; if it matters, re-read by hash after insert and return the oldest row.
     *  - expiresAt = now + ttlSeconds when ttlSeconds is given.
     *  - write-through to {@link UrlCache}.
     */
    public ShortenResult shorten(ShortenRequest request) {
        if (request.customAlias() != null) {
            aliasPolicy.requireAllowed(request.customAlias());
        }
        throw new NotImplementedYetException("UrlShortenerService.shorten");
    }

    /**
     * Planned flow (hot path, 200ms budget). Cache-aside: Redis first, MySQL is the source of truth.
     *  1. {@link UrlCache#get}: a hit returns the long URL straight away. A hit is never stale-expired,
     *     because step 4 caps the cache TTL at the short URL's remaining lifetime.
     *  2. Cache miss, or any Redis error (the cache reports errors as a miss): read
     *     {@link UrlMappingRepository#findByShortCode} from MySQL.
     *  3. No row: {@link ShortUrlNotFoundException} (404). Row expired: {@link ShortUrlExpiredException} (410),
     *     and nothing is cached.
     *  4. Row valid: {@link UrlCache#put} with TTL = min(remaining lifetime, redis.defaultTtlSeconds) so the next
     *     request is a cache hit, then return the long URL.
     *  5. Record the click via {@link ClickTracker} without blocking the response.
     */
    public URI resolve(String shortCode) {
        throw new NotImplementedYetException("UrlShortenerService.resolve");
    }
}
