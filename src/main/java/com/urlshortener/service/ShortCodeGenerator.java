package com.urlshortener.service;

import com.urlshortener.config.ShortenerConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Turns a long URL into short-code candidates (ADR-0006, ADR-0007).
 *
 * <ul>
 *   <li>attempt 0: Base62(hash(url))</li>
 *   <li>attempts 1..maxRehashAttempts: the hash re-hashed that many times</li>
 *   <li>later attempts: Base62(hash(url + "#" + counter)), the salt/counter fallback</li>
 * </ul>
 * Each candidate is {@code codeLength} characters. Thread-safe.
 */
public class ShortCodeGenerator {

    private final String algorithm;
    private final int codeLength;
    private final int maxRehashAttempts;

    public ShortCodeGenerator(ShortenerConfig config) {
        this.algorithm = config.getHashAlgorithm();
        this.codeLength = config.getCodeLength();
        this.maxRehashAttempts = config.getMaxRehashAttempts();
        newDigest(); // fail fast on an unknown algorithm
    }

    /** Hex digest of the long URL, stored as long_url_hash for duplicate lookups. */
    public String digest(String longUrl) {
        return HexFormat.of().formatHex(hash(longUrl.getBytes(StandardCharsets.UTF_8)));
    }

    /** Candidate code for the given attempt (0-based). Deterministic for the same URL and attempt. */
    public String candidate(String longUrl, int attempt) {
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must be >= 0");
        }
        byte[] bytes;
        if (attempt <= maxRehashAttempts) {
            bytes = hash(longUrl.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < attempt; i++) {
                bytes = hash(bytes);
            }
        } else {
            int counter = attempt - maxRehashAttempts;
            bytes = hash((longUrl + "#" + counter).getBytes(StandardCharsets.UTF_8));
        }
        return Base62.encodeFixed(bytes, codeLength);
    }

    public int maxRehashAttempts() {
        return maxRehashAttempts;
    }

    private byte[] hash(byte[] input) {
        return newDigest().digest(input);
    }

    private MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unsupported hash algorithm: " + algorithm, e);
        }
    }
}
