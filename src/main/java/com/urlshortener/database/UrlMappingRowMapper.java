package com.urlshortener.database;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/** Maps a {@code url_mapping} row to {@link UrlMapping}. */
public class UrlMappingRowMapper implements RowMapper<UrlMapping> {

    @Override
    public UrlMapping map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new UrlMapping(
                rs.getLong("id"),
                rs.getString("short_code"),
                rs.getString("long_url"),
                rs.getString("long_url_hash"),
                rs.getString("user_id"),
                rs.getBoolean("is_custom"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("expires_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
