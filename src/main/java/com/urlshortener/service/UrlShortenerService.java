package com.urlshortener.service;

import com.urlshortener.analytics.ClickTracker;
import com.urlshortener.api.ShortenRequest;
import com.urlshortener.api.ShortenResponse;
import com.urlshortener.cache.UrlCache;
import com.urlshortener.database.DuplicateKeys;
import com.urlshortener.database.UrlMapping;
import com.urlshortener.database.UrlMappingRepository;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Conversion service: long URL to short URL, and short code back to the long URL. */
public class UrlShortenerService {

    /** Upper bound on code candidates per request: 1 + 5 re-hashes + salted attempts. */
    static final int MAX_ATTEMPTS = 20;

    private final UrlMappingRepository repository;
    private final UrlCache cache;
    private final ShortCodeGenerator codeGenerator;
    private final AliasSuggester aliasSuggester;
    private final AliasPolicy aliasPolicy;
    private final ClickTracker clickTracker;
    private final String baseUrl;
    private final Duration defaultCacheTtl;
    private final Clock clock;

    public UrlShortenerService(UrlMappingRepository repository,
                               UrlCache cache,
                               ShortCodeGenerator codeGenerator,
                               AliasSuggester aliasSuggester,
                               AliasPolicy aliasPolicy,
                               ClickTracker clickTracker,
                               String baseUrl,
                               Duration defaultCacheTtl,
                               Clock clock) {
        this.repository = repository;
        this.cache = cache;
        this.codeGenerator = codeGenerator;
        this.aliasSuggester = aliasSuggester;
        this.aliasPolicy = aliasPolicy;
        this.clickTracker = clickTracker;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultCacheTtl = defaultCacheTtl;
        this.clock = clock;
    }

    /**
     * Creates a short URL, or returns the existing one.
     * <ul>
     *   <li>Every short URL belongs to {@code request.userId()}.</li>
     *   <li>Custom alias: globally unique; reserved words are rejected (422); a taken alias, expired or not, gives
     *       {@link AliasTakenException} (409) with suggestions.</li>
     *   <li>No alias: the same user's unexpired generated mapping for the same URL is returned as is
     *       ({@code created=false}, 200; the request's TTL is ignored, ADR-0019). Other users never share it
     *       (ADR-0020). Otherwise code candidates are tried in order and the first insert that succeeds wins
     *       ({@code created=true}, 201).</li>
     * </ul>
     * New mappings are written to the cache straight away.
     */
    public ShortenResult shorten(ShortenRequest request) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS); // TIMESTAMP(3) precision
        String longUrl = request.longUrl().trim();
        String hash = codeGenerator.digest(longUrl);
        Instant expiresAt = request.ttlSeconds() == null ? null : now.plusSeconds(request.ttlSeconds());
        String userId = request.userId();

        if (request.customAlias() != null) {
            return createWithAlias(request.customAlias(), longUrl, hash, userId, now, expiresAt);
        }

        Optional<UrlMapping> existing = repository.findActiveByUserAndLongUrl(userId, hash, longUrl, now);
        if (existing.isPresent()) {
            return new ShortenResult(toResponse(existing.get()), false);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = codeGenerator.candidate(ShortCodeGenerator.key(userId, longUrl), attempt);
            UrlMapping mapping = new UrlMapping(null, code, longUrl, hash, userId, false, now, expiresAt);
            if (tryInsert(mapping)) {
                cacheMapping(mapping, now);
                return new ShortenResult(toResponse(mapping), true);
            }
            // Code taken. If a concurrent request by the same user just stored this URL under it, reuse that row.
            Optional<UrlMapping> holder = repository.findByShortCode(code);
            if (holder.isPresent() && isReusable(holder.get(), userId, longUrl, now)) {
                return new ShortenResult(toResponse(holder.get()), false);
            }
        }
        throw new ShortCodeExhaustedException(MAX_ATTEMPTS);
    }

    private ShortenResult createWithAlias(String alias, String longUrl, String hash, String userId,
                                          Instant now, Instant expiresAt) {
        aliasPolicy.requireAllowed(alias);
        UrlMapping mapping = new UrlMapping(null, alias, longUrl, hash, userId, true, now, expiresAt);
        if (!tryInsert(mapping)) {
            throw new AliasTakenException(alias, aliasSuggester.suggest(alias));
        }
        cacheMapping(mapping, now);
        return new ShortenResult(toResponse(mapping), true);
    }

    /**
     * Redirect lookup (hot path, 200ms budget), cache-aside with MySQL as the source of truth (ADR-0005):
     * Redis first; on a miss or Redis error read MySQL; 404 if unknown, 410 if expired (not cached);
     * otherwise refill the cache for min(remaining lifetime, default cache TTL).
     */
    public URI resolve(String shortCode) {
        String longUrl = cache.get(shortCode).orElseGet(() -> loadFromDatabase(shortCode));
        clickTracker.recordClick(shortCode);
        return URI.create(longUrl);
    }

    private String loadFromDatabase(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        Instant now = clock.instant();
        if (mapping.isExpired(now)) {
            throw new ShortUrlExpiredException(shortCode);
        }
        cacheMapping(mapping, now);
        return mapping.longUrl();
    }

    private boolean tryInsert(UrlMapping mapping) {
        try {
            repository.insert(mapping);
            return true;
        } catch (RuntimeException e) {
            if (DuplicateKeys.isDuplicateKey(e)) {
                return false;
            }
            throw e;
        }
    }

    private static boolean isReusable(UrlMapping mapping, String userId, String longUrl, Instant now) {
        return !mapping.custom() && userId.equals(mapping.userId()) && mapping.longUrl().equals(longUrl)
                && !mapping.isExpired(now);
    }

    private void cacheMapping(UrlMapping mapping, Instant now) {
        cache.put(mapping.shortCode(), mapping.longUrl(), cacheTtl(mapping, now));
    }

    /** Never longer than the short URL's remaining lifetime, so a cache hit is never expired. */
    Duration cacheTtl(UrlMapping mapping, Instant now) {
        if (mapping.expiresAt() == null) {
            return defaultCacheTtl;
        }
        Duration remaining = Duration.between(now, mapping.expiresAt());
        return remaining.compareTo(defaultCacheTtl) < 0 ? remaining : defaultCacheTtl;
    }

    private ShortenResponse toResponse(UrlMapping mapping) {
        return new ShortenResponse(mapping.shortCode(), baseUrl + "/" + mapping.shortCode(), mapping.longUrl(),
                mapping.expiresAt(), mapping.userId());
    }
}
