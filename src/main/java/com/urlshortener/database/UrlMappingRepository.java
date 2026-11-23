package com.urlshortener.database;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

/**
 * Data access for short URL mappings (JDBI SQL Object).
 *
 * The statements below only declare the queries the service will need. No service code calls them yet.
 */
@RegisterRowMapper(UrlMappingRowMapper.class)
public interface UrlMappingRepository {

    @SqlQuery("SELECT * FROM url_mapping WHERE short_code = :shortCode")
    Optional<UrlMapping> findByShortCode(@Bind("shortCode") String shortCode);

    /**
     * Lookup used to return the existing short URL when the same long URL is shortened again, by any user.
     * Matches the hash for the index, then the full URL so a hash collision can never return the wrong link.
     * Oldest row first, so concurrent duplicates converge on one code.
     */
    @SqlQuery("""
            SELECT * FROM url_mapping
            WHERE long_url_hash = :longUrlHash
              AND long_url = :longUrl
              AND is_custom = FALSE
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP(3))
            ORDER BY id
            LIMIT 1
            """)
    Optional<UrlMapping> findActiveByLongUrl(@Bind("longUrlHash") String longUrlHash, @Bind("longUrl") String longUrl);

    @SqlQuery("SELECT COUNT(*) > 0 FROM url_mapping WHERE short_code = :shortCode")
    boolean existsByShortCode(@Bind("shortCode") String shortCode);

    /** Insert relies on the UNIQUE index on short_code as the final guard against duplicates. */
    @SqlUpdate("""
            INSERT INTO url_mapping (short_code, long_url, long_url_hash, user_id, is_custom, created_at, expires_at)
            VALUES (:shortCode, :longUrl, :longUrlHash, :userId, :custom, :createdAt, :expiresAt)
            """)
    @GetGeneratedKeys("id")
    long insert(@BindMethods UrlMapping mapping);
}
