package com.xinyu.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run with {@code -Dit.postgres=true}; it only talks to the disposable Testcontainers database.
 */
@EnabledIfSystemProperty(named = "it.postgres", matches = "true")
class FlywayPostgresIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeAll
    static void startContainer() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @Test
    void flywayCreatesUsersAndRefreshTokenTables() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
             ResultSet tables = connection.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
            Set<String> tableNames = tableNames(tables);
            assertThat(tableNames).contains("users", "refresh_tokens", "categories", "tags",
                    "articles", "article_tags", "comments");
        }
    }

    private Set<String> tableNames(ResultSet tables) throws Exception {
        Set<String> result = new HashSet<>();
        while (tables.next()) {
            result.add(tables.getString("TABLE_NAME"));
        }
        return result;
    }
}
