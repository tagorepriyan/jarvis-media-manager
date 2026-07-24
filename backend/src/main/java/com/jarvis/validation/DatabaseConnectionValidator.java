package com.jarvis.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseConnectionValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionValidator.class);

    private final DataSource dataSource;

    public DatabaseConnectionValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Verifying database connection...");
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                logger.info("Database connection is valid.");
            } else {
                throw new SQLException("Database connection is not valid.");
            }
        } catch (SQLException e) {
            logger.error("************************************************************");
            logger.error("DATABASE CONNECTION ERROR");
            logger.error("Could not connect to the database. Please check your configuration.");
            logger.error("URL: " + getUrlFromDataSource());
            logger.error("Username: " + getUsernameFromDataSource());
            logger.error("Please ensure the following:");
            logger.error("1. The database server is running.");
            logger.error("2. The connection URL, username, and password are correct.");
            logger.error("3. The database specified in the URL exists.");
            logger.error("4. The user has the necessary permissions.");
            logger.error("Underlying exception: " + e.getMessage());
            logger.error("************************************************************");
            throw new IllegalStateException("Database connection failed", e);
        }
    }

    private String getUrlFromDataSource() {
        try {
            return dataSource.unwrap(com.zaxxer.hikari.HikariDataSource.class).getJdbcUrl();
        } catch (Exception e) {
            //ignore
        }
        return "N/A";
    }

    private String getUsernameFromDataSource() {
        try {
            return dataSource.unwrap(com.zaxxer.hikari.HikariDataSource.class).getUsername();
        } catch (Exception e) {
            //ignore
        }
        return "N/A";
    }
}
