package com.urlshortener.database;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Data access for short URL mappings (JDBI SQL Object). */
@RegisterRowMapper(UrlMappingRowMapper.class)
public interface UrlMappingRepository {

    @SqlQuery("SELECT * FROM url_mapping WHERE short_code = :shortCode")
    Optional<UrlMapping> findByShortCode(@Bind("shortCode") String shortCode);

    /**
     * The existing generated short URL for this long URL, by any user, if it is still valid at {@code now}.
     * Matches the hash for the index, then the full URL so a hash collision can never return the wrong link.
     * Oldest row first, so any duplicates converge on one code.
     */
    @SqlQuery("""
            SELECT * FROM url_mapping
            WHERE long_url_hash = :longUrlHash
              AND long_url = :longUrl
              AND is_custom = FALSE
              AND (expires_at IS NULL OR expires_at > :now)
            ORDER BY id
            LIMIT 1
            """)
    Optional<UrlMapping> findActiveByLongUrl(@Bind("longUrlHash") String longUrlHash,
                                             @Bind("longUrl") String longUrl,
                                             @Bind("now") Instant now);

    /** The subset of {@code shortCodes} already stored (expired or not). Must not be called with an empty list. */
    @SqlQuery("SELECT short_code FROM url_mapping WHERE short_code IN (<shortCodes>)")
    Set<String> findExistingCodes(@BindList("shortCodes") Collection<String> shortCodes);

    /**
     * Inserts a mapping. The UNIQUE index on short_code is the uniqueness check: a taken code fails with a
     * duplicate-key error (see {@link DuplicateKeys}), which also settles races between concurrent requests.
     */
    @SqlUpdate("""
            INSERT INTO url_mapping (short_code, long_url, long_url_hash, user_id, is_custom, created_at, expires_at)
            VALUES (:shortCode, :longUrl, :longUrlHash, :userId, :custom, :createdAt, :expiresAt)
            """)
    @GetGeneratedKeys("id")
    long insert(@BindMethods UrlMapping mapping);
}
