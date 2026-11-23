package com.urlshortener.database;

import java.time.Instant;

/**
 * One row of the {@code url_mapping} table: a short code and the long URL it points to.
 *
 * @param id          surrogate key
 * @param shortCode   7-char Base62 code (generated or custom alias), unique
 * @param longUrl     original URL
 * @param longUrlHash hex digest of the long URL, used to find an existing short URL for the same input
 * @param userId      owner; every short URL belongs to exactly one user (ADR-0020)
 * @param custom      true when the code is a user-supplied alias
 * @param createdAt   creation time
 * @param expiresAt   null means it never expires
 */
public record UrlMapping(
        Long id,
        String shortCode,
        String longUrl,
        String longUrlHash,
        String userId,
        boolean custom,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }
}
