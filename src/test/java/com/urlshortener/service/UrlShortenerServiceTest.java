package com.urlshortener.service;

import com.urlshortener.api.ShortenRequest;
import com.urlshortener.cache.UrlCache;
import com.urlshortener.config.ShortenerConfig;
import com.urlshortener.database.UrlMapping;
import com.urlshortener.database.UrlMappingRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlShortenerServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final String URL = "https://example.com/page";
    private static final String USER = "u1";
    private static final String KEY = ShortCodeGenerator.key(USER, URL);

    private final FakeRepository repository = new FakeRepository();
    private final FakeCache cache = new FakeCache();
    private final List<String> clicks = new ArrayList<>();
    private final ShortCodeGenerator generator = new ShortCodeGenerator(new ShortenerConfig());
    private final AliasPolicy policy = new AliasPolicy(Set.of("docs"));
    private final UrlShortenerService service = new UrlShortenerService(
            repository, cache, generator,
            new AliasSuggester(3, policy, repository::findExistingCodes, new Random(1)),
            policy, clicks::add, "http://sho.rt/", DEFAULT_TTL, Clock.fixed(NOW, ZoneOffset.UTC));

    private static ShortenRequest request(String url, String alias, Long ttl, String user) {
        return new ShortenRequest(url, alias, ttl, user);
    }

    private UrlMapping row(String code, String url, boolean custom, Instant expiresAt) {
        return row(code, url, USER, custom, expiresAt);
    }

    private UrlMapping row(String code, String url, String user, boolean custom, Instant expiresAt) {
        return new UrlMapping(null, code, url, generator.digest(url), user, custom, NOW.minusSeconds(60), expiresAt);
    }

    @Nested
    class Shorten {

        @Test
        void createsGeneratedCodeWith201AndCachesIt() {
            ShortenResult result = service.shorten(request(URL, null, null, "u1"));
            assertThat(result.created()).isTrue();
            assertThat(result.response().shortCode()).isEqualTo(generator.candidate(KEY, 0));
            assertThat(result.response().shortUrl()).isEqualTo("http://sho.rt/" + result.response().shortCode());
            assertThat(result.response().userId()).isEqualTo("u1");
            assertThat(result.response().expiresAt()).isNull();
            assertThat(cache.values).containsEntry(result.response().shortCode(), URL);
            assertThat(cache.ttls).containsEntry(result.response().shortCode(), DEFAULT_TTL);
        }

        @Test
        void ttlSetsExpiryAndCapsCacheTtl() {
            ShortenResult result = service.shorten(request(URL, null, 3600L, USER));
            assertThat(result.response().expiresAt()).isEqualTo(NOW.plusSeconds(3600));
            assertThat(cache.ttls).containsEntry(result.response().shortCode(), Duration.ofHours(1));
        }

        @Test
        void sameUserAndUrlReusesExistingCodeWith200AndIgnoresNewTtl() {
            ShortenResult first = service.shorten(request(URL, null, null, "u1"));
            ShortenResult second = service.shorten(request(URL, null, 600L, "u1"));
            assertThat(second.created()).isFalse();
            assertThat(second.response().shortCode()).isEqualTo(first.response().shortCode());
            assertThat(second.response().expiresAt()).isNull();
            assertThat(repository.rows).hasSize(1);
        }

        @Test
        void differentUserGetsTheirOwnShortUrlForTheSameUrl() {
            ShortenResult a = service.shorten(request(URL, null, null, "u1"));
            ShortenResult b = service.shorten(request(URL, null, null, "u2"));
            assertThat(b.created()).isTrue();
            assertThat(b.response().shortCode()).isNotEqualTo(a.response().shortCode())
                    .isEqualTo(generator.candidate(ShortCodeGenerator.key("u2", URL), 0));
            assertThat(b.response().userId()).isEqualTo("u2");
            assertThat(repository.rows.get(a.response().shortCode()).userId()).isEqualTo("u1");
            assertThat(repository.rows.get(b.response().shortCode()).userId()).isEqualTo("u2");
        }

        @Test
        void otherUsersRowHoldingTheCandidateIsNotReused() {
            String first = generator.candidate(KEY, 0);
            repository.hideFromReuseLookup = true;
            repository.store(row(first, URL, "someone-else", false, null));
            ShortenResult result = service.shorten(request(URL, null, null, USER));
            assertThat(result.created()).isTrue();
            assertThat(result.response().shortCode()).isEqualTo(generator.candidate(KEY, 1));
            assertThat(result.response().userId()).isEqualTo(USER);
        }

        @Test
        void collisionWithAnotherUrlMovesToNextCandidate() {
            String first = generator.candidate(KEY, 0);
            repository.store(row(first, "https://other.example/x", false, null));
            ShortenResult result = service.shorten(request(URL, null, null, USER));
            assertThat(result.created()).isTrue();
            assertThat(result.response().shortCode()).isEqualTo(generator.candidate(KEY, 1));
        }

        @Test
        void afterFiveRehashCollisionsUsesSaltedCandidate() {
            for (int attempt = 0; attempt <= 5; attempt++) {
                repository.store(row(generator.candidate(KEY, attempt), "https://other.example/" + attempt, false, null));
            }
            ShortenResult result = service.shorten(request(URL, null, null, USER));
            assertThat(result.response().shortCode()).isEqualTo(generator.candidate(KEY, 6));
        }

        @Test
        void expiredCodeStaysTakenSoSameUrlGetsANewCode() {
            String first = generator.candidate(KEY, 0);
            repository.store(row(first, URL, false, NOW.minusSeconds(1)));
            ShortenResult result = service.shorten(request(URL, null, null, USER));
            assertThat(result.created()).isTrue();
            assertThat(result.response().shortCode()).isNotEqualTo(first).isEqualTo(generator.candidate(KEY, 1));
        }

        @Test
        void concurrentInsertOfSameUrlIsReusedNotDuplicated() {
            String first = generator.candidate(KEY, 0);
            repository.hideFromReuseLookup = true; // simulates the other request inserting after our lookup
            repository.store(row(first, URL, false, null));
            ShortenResult result = service.shorten(request(URL, null, null, USER));
            assertThat(result.created()).isFalse();
            assertThat(result.response().shortCode()).isEqualTo(first);
            assertThat(repository.rows).hasSize(1);
        }

        @Test
        void givesUpWith503AfterMaxAttempts() {
            for (int attempt = 0; attempt < UrlShortenerService.MAX_ATTEMPTS; attempt++) {
                repository.store(row(generator.candidate(KEY, attempt), "https://other.example/" + attempt, false, null));
            }
            assertThatThrownBy(() -> service.shorten(request(URL, null, null, USER)))
                    .isInstanceOf(ShortCodeExhaustedException.class);
        }

        @Test
        void freeCustomAliasIsUsed() {
            ShortenResult result = service.shorten(request(URL, "myLink1", null, USER));
            assertThat(result.created()).isTrue();
            assertThat(result.response().shortCode()).isEqualTo("myLink1");
            assertThat(repository.rows.get("myLink1").custom()).isTrue();
            assertThat(cache.values).containsEntry("myLink1", URL);
        }

        @Test
        void takenCustomAliasThrows409WithThreeFreeSuggestions() {
            repository.store(row("myLink", "https://other.example", true, null));
            repository.store(row("myLink1", "https://other.example/1", true, null));
            assertThatThrownBy(() -> service.shorten(request(URL, "myLink", null, USER)))
                    .isInstanceOfSatisfying(AliasTakenException.class, e ->
                            assertThat(e.suggestions()).containsExactly("myLink2", "myLink3", "myLink4"));
        }

        @Test
        void expiredCustomAliasStaysTaken() {
            repository.store(row("oldOne", "https://other.example", true, NOW.minusSeconds(1)));
            assertThatThrownBy(() -> service.shorten(request(URL, "oldOne", null, USER)))
                    .isInstanceOf(AliasTakenException.class);
        }

        @Test
        void reservedAliasIsRejected() {
            assertThatThrownBy(() -> service.shorten(request(URL, "Docs", null, USER)))
                    .isInstanceOf(ReservedAliasException.class);
            assertThat(repository.rows).isEmpty();
        }

        @Test
        void customAliasIsNotReusedForSameUrlWithoutAlias() {
            service.shorten(request(URL, "myLink1", null, USER));
            ShortenResult generated = service.shorten(request(URL, null, null, USER));
            assertThat(generated.created()).isTrue();
            assertThat(generated.response().shortCode()).isNotEqualTo("myLink1");
        }
    }

    @Nested
    class Resolve {

        @Test
        void cacheHitDoesNotTouchTheDatabase() {
            cache.values.put("abc1234", "https://example.com/hit");
            assertThat(service.resolve("abc1234")).isEqualTo(URI.create("https://example.com/hit"));
            assertThat(repository.lookups).isZero();
            assertThat(clicks).containsExactly("abc1234");
        }

        @Test
        void cacheMissReadsDatabaseAndRefillsCache() {
            repository.store(row("abc1234", URL, false, null));
            assertThat(service.resolve("abc1234")).isEqualTo(URI.create(URL));
            assertThat(repository.lookups).isEqualTo(1);
            assertThat(cache.values).containsEntry("abc1234", URL);
            assertThat(cache.ttls).containsEntry("abc1234", DEFAULT_TTL);
        }

        @Test
        void cacheTtlNeverOutlivesTheShortUrl() {
            repository.store(row("abc1234", URL, false, NOW.plusSeconds(30)));
            service.resolve("abc1234");
            assertThat(cache.ttls).containsEntry("abc1234", Duration.ofSeconds(30));
        }

        @Test
        void unknownCodeIsNotFound() {
            assertThatThrownBy(() -> service.resolve("nope123")).isInstanceOf(ShortUrlNotFoundException.class);
            assertThat(clicks).isEmpty();
        }

        @Test
        void expiredCodeIsGoneAndNotCached() {
            repository.store(row("old1234", URL, false, NOW));
            assertThatThrownBy(() -> service.resolve("old1234")).isInstanceOf(ShortUrlExpiredException.class);
            assertThat(cache.values).isEmpty();
        }
    }

    static class FakeCache implements UrlCache {
        final Map<String, String> values = new HashMap<>();
        final Map<String, Duration> ttls = new HashMap<>();

        @Override
        public Optional<String> get(String shortCode) {
            return Optional.ofNullable(values.get(shortCode));
        }

        @Override
        public void put(String shortCode, String longUrl, Duration ttl) {
            values.put(shortCode, longUrl);
            ttls.put(shortCode, ttl);
        }

        @Override
        public void evict(String shortCode) {
            values.remove(shortCode);
        }
    }

    /** In-memory repository that fails duplicate inserts the way MySQL's unique index does. */
    static class FakeRepository implements UrlMappingRepository {
        final Map<String, UrlMapping> rows = new HashMap<>();
        int lookups;
        boolean hideFromReuseLookup;

        void store(UrlMapping m) {
            rows.put(m.shortCode(), m);
        }

        @Override
        public Optional<UrlMapping> findByShortCode(String shortCode) {
            lookups++;
            return Optional.ofNullable(rows.get(shortCode));
        }

        @Override
        public Optional<UrlMapping> findActiveByUserAndLongUrl(String userId, String longUrlHash, String longUrl,
                                                             Instant now) {
            if (hideFromReuseLookup) {
                return Optional.empty();
            }
            return rows.values().stream()
                    .filter(m -> !m.custom() && userId.equals(m.userId()))
                    .filter(m -> m.longUrlHash().equals(longUrlHash) && m.longUrl().equals(longUrl))
                    .filter(m -> !m.isExpired(now))
                    .findFirst();
        }

        @Override
        public Set<String> findExistingCodes(Collection<String> shortCodes) {
            Set<String> hit = new HashSet<>(shortCodes);
            hit.retainAll(rows.keySet());
            return hit;
        }

        @Override
        public long insert(UrlMapping mapping) {
            if (rows.containsKey(mapping.shortCode())) {
                throw new RuntimeException(new SQLIntegrityConstraintViolationException("Duplicate entry"));
            }
            store(mapping);
            return rows.size();
        }
    }
}
