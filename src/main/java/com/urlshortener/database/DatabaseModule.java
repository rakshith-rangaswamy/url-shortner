package com.urlshortener.database;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

/**
 * Owns the MySQL connection: runs schema migrations, then builds a pooled {@link Jdbi}.
 *
 * Dropwizard registers a health check named "mysql" and pool metrics for the data source automatically.
 */
public final class DatabaseModule {

    private DatabaseModule() {
    }

    public static Jdbi connect(DataSourceFactory database, Environment environment) {
        migrate(database);
        return new JdbiFactory().build(environment, database, "mysql");
    }

    /** Applies the SQL files in src/main/resources/db/migration. */
    static void migrate(DataSourceFactory database) {
        Flyway.configure()
                .dataSource(database.getUrl(), database.getUser(), database.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
