package com.urlshortener.support;

import org.jdbi.v3.core.Jdbi;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

/**
 * MySQL and Redis containers shared by all end-to-end tests in one JVM.
 * Started once on first use and stopped by Testcontainers when the JVM exits.
 */
public final class Infrastructure {

    public static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("urlshortener")
            .withUsername("urlshortener")
            .withPassword("urlshortener");

    public static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    private Infrastructure() {
    }

    public static Jdbi jdbi() {
        return Jdbi.create(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    public static Jedis redis() {
        return new Jedis(REDIS.getHost(), REDIS.getMappedPort(6379));
    }
}
