package com.querybuilder.backend.datasource.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.model.SchemaCache;
import com.querybuilder.backend.datasource.repository.SchemaCacheRepository;
import com.querybuilder.backend.shared.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for discovering and caching database schemas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaDiscoveryService {

    private final SchemaCacheRepository schemaCacheRepository;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;
    private final ConnectionTestService connectionTestService;

    private static final int CACHE_DURATION_HOURS = 1;

    /**
     * Get database schema (from cache or fetch fresh)
     */
    @Transactional
    public Map<String, Object> getSchema(DataSource dataSource) {
        log.info("Getting schema for data source: {}", dataSource.getId());

        // Try to get from cache first
        Optional<SchemaCache> cachedSchema = schemaCacheRepository
                .findValidCacheByDataSource(dataSource, LocalDateTime.now());

        if (cachedSchema.isPresent()) {
            log.info("Returning cached schema for data source: {}", dataSource.getId());
            return parseSchemaJson(cachedSchema.get().getSchemaData());
        }

        // Fetch fresh schema
        log.info("Cache miss - fetching fresh schema for data source: {}", dataSource.getId());
        Map<String, Object> schema = discoverSchema(dataSource);

        // Cache the schema
        cacheSchema(dataSource, schema);

        return schema;
    }

    /**
     * Discover database schema by connecting to database
     */
    private Map<String, Object> discoverSchema(DataSource dataSource) {
        String decryptedPassword = encryptionUtil.decrypt(dataSource.getPassword());

        String jdbcUrl = buildJdbcUrl(dataSource);

        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, dataSource.getUsername(), decryptedPassword)) {

            DatabaseMetaData metaData = connection.getMetaData();

            Map<String, Object> schema = new HashMap<>();
            schema.put("databaseType", dataSource.getDatabaseType().name());
            schema.put("databaseName", dataSource.getDatabaseName());
            schema.put("tables", discoverTables(metaData, dataSource.getDatabaseName()));
            schema.put("discoveredAt", LocalDateTime.now().toString());

            log.info("Schema discovery completed for data source: {}", dataSource.getId());

            return schema;

        } catch (SQLException e) {
            log.error("Failed to discover schema for data source: {}", dataSource.getId(), e);
            throw new RuntimeException("Failed to discover schema: " + e.getMessage(), e);
        }
    }

    /**
     * Discover all tables in the database
     */
    private List<Map<String, Object>> discoverTables(DatabaseMetaData metaData, String databaseName)
            throws SQLException {

        List<Map<String, Object>> tables = new ArrayList<>();

        try (ResultSet tablesResultSet = metaData.getTables(
                databaseName, null, "%", new String[]{"TABLE"})) {

            while (tablesResultSet.next()) {
                String tableName = tablesResultSet.getString("TABLE_NAME");

                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("name", tableName);
                tableInfo.put("type", tablesResultSet.getString("TABLE_TYPE"));
                tableInfo.put("columns", discoverColumns(metaData, databaseName, tableName));
                tableInfo.put("primaryKeys", discoverPrimaryKeys(metaData, databaseName, tableName));
                tableInfo.put("foreignKeys", discoverForeignKeys(metaData, databaseName, tableName));

                tables.add(tableInfo);
            }
        }

        log.debug("Discovered {} tables", tables.size());
        return tables;
    }

    /**
     * Discover columns for a table
     */
    private List<Map<String, Object>> discoverColumns(
            DatabaseMetaData metaData, String databaseName, String tableName) throws SQLException {

        List<Map<String, Object>> columns = new ArrayList<>();

        try (ResultSet columnsResultSet = metaData.getColumns(
                databaseName, null, tableName, "%")) {

            while (columnsResultSet.next()) {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("name", columnsResultSet.getString("COLUMN_NAME"));
                columnInfo.put("type", columnsResultSet.getString("TYPE_NAME"));
                columnInfo.put("size", columnsResultSet.getInt("COLUMN_SIZE"));
                columnInfo.put("nullable", columnsResultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                columnInfo.put("defaultValue", columnsResultSet.getString("COLUMN_DEF"));

                columns.add(columnInfo);
            }
        }

        return columns;
    }

    /**
     * Discover primary keys for a table
     */
    private List<String> discoverPrimaryKeys(
            DatabaseMetaData metaData, String databaseName, String tableName) throws SQLException {

        List<String> primaryKeys = new ArrayList<>();

        try (ResultSet pkResultSet = metaData.getPrimaryKeys(databaseName, null, tableName)) {
            while (pkResultSet.next()) {
                primaryKeys.add(pkResultSet.getString("COLUMN_NAME"));
            }
        }

        return primaryKeys;
    }

    /**
     * Discover foreign keys for a table
     */
    private List<Map<String, Object>> discoverForeignKeys(
            DatabaseMetaData metaData, String databaseName, String tableName) throws SQLException {

        List<Map<String, Object>> foreignKeys = new ArrayList<>();

        try (ResultSet fkResultSet = metaData.getImportedKeys(databaseName, null, tableName)) {
            while (fkResultSet.next()) {
                Map<String, Object> fkInfo = new HashMap<>();
                fkInfo.put("columnName", fkResultSet.getString("FKCOLUMN_NAME"));
                fkInfo.put("referencedTable", fkResultSet.getString("PKTABLE_NAME"));
                fkInfo.put("referencedColumn", fkResultSet.getString("PKCOLUMN_NAME"));

                foreignKeys.add(fkInfo);
            }
        }

        return foreignKeys;
    }

    /**
     * Cache schema in database
     */
    private void cacheSchema(DataSource dataSource, Map<String, Object> schema) {
        try {
            String schemaJson = objectMapper.writeValueAsString(schema);

            // Delete old cache if exists
            schemaCacheRepository.deleteByDataSource(dataSource);

            // Create new cache
            SchemaCache schemaCache = SchemaCache.builder()
                    .dataSource(dataSource)
                    .schemaData(schemaJson)
                    .cachedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(CACHE_DURATION_HOURS))
                    .build();

            schemaCacheRepository.save(schemaCache);
            log.info("Schema cached for data source: {}", dataSource.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize schema to JSON", e);
        }
    }

    /**
     * Parse schema JSON string to Map
     */
    private Map<String, Object> parseSchemaJson(String schemaJson) {
        try {
            return objectMapper.readValue(schemaJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse schema JSON", e);
            throw new RuntimeException("Failed to parse cached schema", e);
        }
    }

    /**
     * Build JDBC URL
     */
    private String buildJdbcUrl(DataSource dataSource) {
        String sslParam = Boolean.TRUE.equals(dataSource.getSslEnabled()) ? "&useSSL=true" : "&useSSL=false";

        return switch (dataSource.getDatabaseType()) {
            case MYSQL -> String.format(
                    "jdbc:mysql://%s:%d/%s?serverTimezone=UTC%s",
                    dataSource.getHost(), dataSource.getPort(),
                    dataSource.getDatabaseName(), sslParam
            );

            case POSTGRESQL -> String.format(
                    "jdbc:postgresql://%s:%d/%s?ssl=%s",
                    dataSource.getHost(), dataSource.getPort(),
                    dataSource.getDatabaseName(), dataSource.getSslEnabled() ? "true" : "false"
            );

            case SQLSERVER -> String.format(
                    "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s",
                    dataSource.getHost(), dataSource.getPort(),
                    dataSource.getDatabaseName(), dataSource.getSslEnabled() ? "true" : "false"
            );

            case ORACLE -> String.format(
                    "jdbc:oracle:thin:@%s:%d:%s",
                    dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName()
            );
        };
    }

    /**
     * Clear cache for a data source
     */
    @Transactional
    public void clearCache(DataSource dataSource) {
        schemaCacheRepository.deleteByDataSource(dataSource);
        log.info("Cache cleared for data source: {}", dataSource.getId());
    }
}