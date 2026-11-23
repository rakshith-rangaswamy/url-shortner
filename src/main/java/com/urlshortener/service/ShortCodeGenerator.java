package com.urlshortener.service;

import com.urlshortener.config.ShortenerConfig;

/**
 * Turns a long URL into a short code.
 *
 * Planned algorithm (agreed design, not implemented):
 *  1. digest = hash(longUrl [+ userId]) using {@link ShortenerConfig#getHashAlgorithm()}
 *  2. code   = first {@link ShortenerConfig#getCodeLength()} chars of Base62(digest)
 *  3. on collision with a different URL, re-hash (attempt 1..maxRehashAttempts)
 *  4. still colliding: append a salt/counter and hash again
 */
public class ShortCodeGenerator {

    private final ShortenerConfig config;

    public ShortCodeGenerator(ShortenerConfig config) {
        this.config = config;
    }

    /** Hex digest of the long URL, stored as long_url_hash for duplicate lookups. */
    public String digest(String longUrl) {
        throw new NotImplementedYetException("ShortCodeGenerator.digest");
    }

    /**
     * Candidate code for the given attempt. Attempt 0 is the plain hash, 1..maxRehashAttempts are re-hashes,
     * anything above falls back to salt/counter.
     */
    public String candidate(String longUrl, String userId, int attempt) {
        throw new NotImplementedYetException("ShortCodeGenerator.candidate");
    }

    public ShortenerConfig config() {
        return config;
    }
}
