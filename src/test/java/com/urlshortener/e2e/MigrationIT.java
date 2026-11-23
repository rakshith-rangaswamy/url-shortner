package com.urlshortener.e2e;

import com.urlshortener.support.Infrastructure;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the migrations step by step on a scratch database to check V2's backfill of pre-existing rows. */
class MigrationIT {

    @Test
    void v2AssignsRowsWithoutUserToAnonymousAndKeepsThem() {
        String root = Infrastructure.MYSQL.getJdbcUrl().replace("/urlshortener", "/");
        Jdbi admin = Jdbi.create(root, "root", Infrastructure.MYSQL.getPassword());
        admin.useHandle(h -> h.execute("CREATE DATABASE IF NOT EXISTS migration_check"));
        String url = Infrastructure.MYSQL.getJdbcUrl().replace("/urlshortener", "/migration_check");

        Flyway.configure().dataSource(url, "root", Infrastructure.MYSQL.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(url, "root", Infrastructure.MYSQL.getPassword())
                .locations("classpath:db/migration").target("1").load().migrate();

        Jdbi db = Jdbi.create(url, "root", Infrastructure.MYSQL.getPassword());
        db.useHandle(h -> h.execute("""
                INSERT INTO url_mapping (short_code, long_url, long_url_hash, user_id, is_custom)
                VALUES ('old0001', 'https://example.com/old', 'h1', NULL, FALSE),
                       ('old0002', 'https://example.com/kept', 'h2', 'alice', FALSE)
                """));

        Flyway.configure().dataSource(url, "root", Infrastructure.MYSQL.getPassword())
                .locations("classpath:db/migration").load().migrate();

        assertThat(db.<String, RuntimeException>withHandle(h -> h.createQuery(
                        "SELECT user_id FROM url_mapping WHERE short_code = 'old0001'").mapTo(String.class).one()))
                .isEqualTo("anonymous");
        assertThat(db.<String, RuntimeException>withHandle(h -> h.createQuery(
                        "SELECT user_id FROM url_mapping WHERE short_code = 'old0002'").mapTo(String.class).one()))
                .isEqualTo("alice");
        assertThat(db.<Integer, RuntimeException>withHandle(h -> h.createQuery(
                        "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                                + "AND table_name = 'url_mapping' AND index_name = 'idx_url_mapping_user_hash'")
                .mapTo(Integer.class).one())).isEqualTo(2);
    }
}
