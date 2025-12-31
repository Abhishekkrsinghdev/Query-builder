package com.querybuilder.backend.datasource.service;

import com.querybuilder.backend.datasource.dto.ConnectionTestResponse;
import com.querybuilder.backend.datasource.model.DatabaseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Service for testing database connections
 */
@Service
@Slf4j
public class ConnectionTestService {

    private static final int CONNECTION_TIMEOUT_SECONDS = 10;

    /**
     * Test database connection
     */
    public ConnectionTestResponse testConnection(
            DatabaseType databaseType,
            String host,
            Integer port,
            String databaseName,
            String username,
            String password,
            Boolean sslEnabled
    ) {
        long startTime = System.currentTimeMillis();

        try {
            String jdbcUrl = buildJdbcUrl(databaseType, host, port, databaseName, sslEnabled);

            log.debug("Testing connection to: {}", jdbcUrl);

            // Set connection properties
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("connectTimeout", String.valueOf(CONNECTION_TIMEOUT_SECONDS * 1000));

            // Try to establish connection
            try (Connection connection = DriverManager.getConnection(jdbcUrl, props)) {
                if (connection.isValid(CONNECTION_TIMEOUT_SECONDS)) {
                    long responseTime = System.currentTimeMillis() - startTime;

                    log.info("Connection test successful for {} - Response time: {}ms",
                            databaseType, responseTime);

                    return ConnectionTestResponse.builder()
                            .success(true)
                            .message("Connection successful")
                            .responseTimeMs(responseTime)
                            .build();
                } else {
                    throw new SQLException("Connection validation failed");
                }
            }

        } catch (SQLException e) {
            long responseTime = System.currentTimeMillis() - startTime;

            log.error("Connection test failed: {}", e.getMessage());

            return ConnectionTestResponse.builder()
                    .success(false)
                    .message("Connection failed: " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    /**
     * Build JDBC URL based on database type
     */
    private String buildJdbcUrl(
            DatabaseType databaseType,
            String host,
            Integer port,
            String databaseName,
            Boolean sslEnabled
    ) {
        String sslParam = Boolean.TRUE.equals(sslEnabled) ? "&useSSL=true" : "&useSSL=false";

        return switch (databaseType) {
            case MYSQL -> String.format(
                    "jdbc:mysql://%s:%d/%s?serverTimezone=UTC%s",
                    host, port, databaseName, sslParam
            );

            case POSTGRESQL -> String.format(
                    "jdbc:postgresql://%s:%d/%s?ssl=%s",
                    host, port, databaseName, sslEnabled ? "true" : "false"
            );

            case SQLSERVER -> String.format(
                    "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s",
                    host, port, databaseName, sslEnabled ? "true" : "false"
            );

            case ORACLE -> String.format(
                    "jdbc:oracle:thin:@%s:%d:%s",
                    host, port, databaseName
            );
        };
    }
}