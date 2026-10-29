package com.developershub.backend.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the integration-test database if it does not exist, before the
 * Spring context (and Flyway) boots. Runs against the same Postgres instance
 * used locally by docker-compose and by the CI service container.
 */
public class TestDatabaseInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String TEST_DB = "developershub_test";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "devuser", "devpass");
             Statement statement = connection.createStatement()) {
            try {
                statement.executeUpdate("CREATE DATABASE " + TEST_DB);
            } catch (SQLException e) {
                // 42P04 = duplicate_database: the database already exists, which is fine.
                if (!"42P04".equals(e.getSQLState())) {
                    throw e;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not prepare test database - is Postgres running on localhost:5432?", e);
        }
    }
}
