package com.urlshortener.service;

import com.urlshortener.analytics.ClickTracker;
import com.urlshortener.api.ShortenRequest;
import com.urlshortener.api.ShortenResponse;
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
    private final ClickTracker clickTracker;

    public UrlShortenerService(UrlMappingRepository repository,
                               UrlCache cache,
                               ShortCodeGenerator codeGenerator,
                               AliasSuggester aliasSuggester,
                               ClickTracker clickTracker) {
        this.repository = repository;
        this.cache = cache;
        this.codeGenerator = codeGenerator;
        this.aliasSuggester = aliasSuggester;
        this.clickTracker = clickTracker;
    }

    /**
     * Planned flow:
     *  - customAlias given: if taken, throw {@link AliasTakenException} with suggestions, else store it.
     *  - otherwise: if an unexpired mapping exists for (longUrlHash, userId), return it;
     *    else generate a code via {@link ShortCodeGenerator} with the collision policy and store it.
     *  - expiresAt = now + ttlSeconds when ttlSeconds is given.
     *  - write-through to {@link UrlCache}.
     */
    public ShortenResponse shorten(ShortenRequest request) {
        throw new NotImplementedYetException("UrlShortenerService.shorten");
    }

    /**
     * Planned flow (hot path, 200ms budget):
     *  - read {@link UrlCache}; on miss read the repository and populate the cache.
     *  - unknown code: {@link ShortUrlNotFoundException} (404); expired: {@link ShortUrlExpiredException} (410).
     *  - record the click via {@link ClickTracker} without blocking the response.
     */
    public URI resolve(String shortCode) {
        throw new NotImplementedYetException("UrlShortenerService.resolve");
    }
}
