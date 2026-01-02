package com.querybuilder.backend.query.service;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.repository.DataSourceRepository;
import com.querybuilder.backend.query.dto.ExecuteQueryRequest;
import com.querybuilder.backend.query.dto.QueryExecutionResponse;
import com.querybuilder.backend.query.dto.QueryHistoryResponse;
import com.querybuilder.backend.query.model.ExecutionStatus;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryExecution;
import com.querybuilder.backend.query.repository.QueryExecutionRepository;
import com.querybuilder.backend.query.repository.QueryRepository;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import com.querybuilder.backend.shared.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for executing queries and managing execution history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExecutionService {

    private final QueryExecutionRepository executionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final QueryRepository queryRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Execute a query
     */
    @Transactional
    public QueryExecutionResponse executeQuery(ExecuteQueryRequest request, User user) {
        log.info("Executing query for user: {}", user.getEmail());

        long startTime = System.currentTimeMillis();

        // Get data source
        DataSource dataSource = dataSourceRepository
                .findByIdAndUserAndDeletedFalse(request.getDataSourceId(), user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DataSource", "id", request.getDataSourceId()));

        QueryExecution execution = QueryExecution.builder()
                .user(user)
                .dataSource(dataSource)
                .sqlQuery(request.getSqlQuery())
                .executedAt(LocalDateTime.now())
                .build();

        try {
            // Apply parameter substitution if needed
            String finalSql = applyParameters(request.getSqlQuery(), request.getParameters());

            // Apply limit if specified
            if (request.getLimit() != null) {
                finalSql = applyLimit(finalSql, request.getLimit());
            }

            // Execute query
            QueryExecutionResponse.QueryExecutionResponseBuilder responseBuilder =
                    QueryExecutionResponse.builder();

            String decryptedPassword = encryptionUtil.decrypt(dataSource.getPassword());
            String jdbcUrl = buildJdbcUrl(dataSource);

            try (Connection connection = DriverManager.getConnection(
                    jdbcUrl, dataSource.getUsername(), decryptedPassword)) {

                // Set timeout
                try (Statement statement = connection.createStatement()) {
                    if (request.getTimeout() != null) {
                        statement.setQueryTimeout(request.getTimeout());
                    }

                    boolean hasResultSet = statement.execute(finalSql);

                    if (hasResultSet) {
                        try (ResultSet resultSet = statement.getResultSet()) {
                            ResultSetMetaData metaData = resultSet.getMetaData();
                            int columnCount = metaData.getColumnCount();

                            // Extract column information
                            List<QueryExecutionResponse.ColumnInfo> columns = new ArrayList<>();
                            for (int i = 1; i <= columnCount; i++) {
                                columns.add(QueryExecutionResponse.ColumnInfo.builder()
                                        .name(metaData.getColumnName(i))
                                        .type(metaData.getColumnTypeName(i))
                                        .nullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable)
                                        .build());
                            }

                            // Extract rows
                            List<Map<String, Object>> rows = new ArrayList<>();
                            int rowCount = 0;

                            while (resultSet.next() && rowCount < request.getLimit()) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                for (int i = 1; i <= columnCount; i++) {
                                    row.put(metaData.getColumnName(i), resultSet.getObject(i));
                                }
                                rows.add(row);
                                rowCount++;
                            }

                            long executionTime = System.currentTimeMillis() - startTime;

                            execution.setExecutionStatus(ExecutionStatus.SUCCESS);
                            execution.setExecutionTimeMs((int) executionTime);
                            execution.setRowsReturned(rowCount);

                            responseBuilder
                                    .status(ExecutionStatus.SUCCESS)
                                    .columns(columns)
                                    .rows(rows)
                                    .rowsReturned(rowCount)
                                    .executionTimeMs((int) executionTime);

                            log.info("Query executed successfully - Rows: {}, Time: {}ms",
                                    rowCount, executionTime);
                        }
                    } else {
                        // Query didn't return a result set (e.g., UPDATE, DELETE)
                        int updateCount = statement.getUpdateCount();
                        long executionTime = System.currentTimeMillis() - startTime;

                        execution.setExecutionStatus(ExecutionStatus.SUCCESS);
                        execution.setExecutionTimeMs((int) executionTime);
                        execution.setRowsReturned(updateCount);

                        responseBuilder
                                .status(ExecutionStatus.SUCCESS)
                                .columns(Collections.emptyList())
                                .rows(Collections.emptyList())
                                .rowsReturned(updateCount)
                                .executionTimeMs((int) executionTime);

                        log.info("Query executed successfully - Rows affected: {}, Time: {}ms",
                                updateCount, executionTime);
                    }
                }
            }

            execution = executionRepository.save(execution);

            return responseBuilder
                    .executionId(execution.getId().toString())
                    .executedAt(execution.getExecutedAt())
                    .build();

        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("Query execution failed: {}", e.getMessage());

            execution.setExecutionStatus(ExecutionStatus.FAILED);
            execution.setExecutionTimeMs((int) executionTime);
            execution.setErrorMessage(e.getMessage());
            execution.setRowsReturned(0);

            executionRepository.save(execution);

            return QueryExecutionResponse.builder()
                    .executionId(execution.getId().toString())
                    .status(ExecutionStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .executionTimeMs((int) executionTime)
                    .rowsReturned(0)
                    .executedAt(execution.getExecutedAt())
                    .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("Unexpected error during query execution", e);

            execution.setExecutionStatus(ExecutionStatus.FAILED);
            execution.setExecutionTimeMs((int) executionTime);
            execution.setErrorMessage("Unexpected error: " + e.getMessage());
            execution.setRowsReturned(0);

            executionRepository.save(execution);

            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a saved query by ID
     */
    @Transactional
    public QueryExecutionResponse executeSavedQuery(UUID queryId, User user, Map<String, Object> parameters) {
        log.info("Executing saved query: {} for user: {}", queryId, user.getEmail());

        Query query = queryRepository.findByIdAndUserAndDeletedFalse(queryId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        ExecuteQueryRequest request = new ExecuteQueryRequest();
        request.setSqlQuery(query.getSqlQuery());
        request.setDataSourceId(query.getDataSource().getId());
        request.setParameters(parameters);
        request.setLimit(1000);
        request.setTimeout(30);

        QueryExecutionResponse response = executeQuery(request, user);

        // Link execution to saved query
        QueryExecution execution = executionRepository.findById(UUID.fromString(response.getExecutionId()))
                .orElseThrow();
        execution.setQuery(query);
        executionRepository.save(execution);

        return response;
    }

    /**
     * Get query execution history for user
     */
    @Transactional(readOnly = true)
    public Page<QueryHistoryResponse> getExecutionHistory(User user, Pageable pageable) {
        log.info("Fetching execution history for user: {}", user.getEmail());

        Page<QueryExecution> executions = executionRepository.findByUserOrderByExecutedAtDesc(user, pageable);

        return executions.map(this::mapHistoryToResponse);
    }

    /**
     * Get recent executions
     */
    @Transactional(readOnly = true)
    public List<QueryHistoryResponse> getRecentExecutions(User user) {
        log.info("Fetching recent executions for user: {}", user.getEmail());

        List<QueryExecution> executions = executionRepository.findTop10ByUserOrderByExecutedAtDesc(user);

        return executions.stream()
                .map(this::mapHistoryToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get execution details by ID
     */
    @Transactional(readOnly = true)
    public QueryHistoryResponse getExecutionById(UUID executionId, User user) {
        log.info("Fetching execution details: {}", executionId);

        QueryExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("QueryExecution", "id", executionId));

        // Verify user owns this execution
        if (!execution.getUser().equals(user)) {
            throw new ResourceNotFoundException("QueryExecution", "id", executionId);
        }

        return mapHistoryToResponse(execution);
    }

    /**
     * Apply parameter substitution to SQL
     */
    private String applyParameters(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }

        String result = sql;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = ":" + entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "NULL";

            // Handle string values with quotes
            if (entry.getValue() instanceof String) {
                value = "'" + value.replace("'", "''") + "'";
            }

            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Apply LIMIT clause to SQL
     */
    private String applyLimit(String sql, Integer limit) {
        if (limit == null) return sql;

        String trimmedSql = sql.trim();

        // Remove trailing semicolon if it exists
        if (trimmedSql.endsWith(";")) {
            trimmedSql = trimmedSql.substring(0, trimmedSql.length() - 1).trim();
        }

        String upperSql = trimmedSql.toUpperCase();

        // Check if LIMIT already exists (consider using regex for better accuracy)
        if (upperSql.contains("LIMIT")) {
            return sql; // Return original if limit is already there
        }

        return trimmedSql + " LIMIT " + limit;
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
     * Map QueryExecution to QueryHistoryResponse
     */
    private QueryHistoryResponse mapHistoryToResponse(QueryExecution execution) {
        return QueryHistoryResponse.builder()
                .id(execution.getId().toString())
                .queryId(execution.getQuery() != null ? execution.getQuery().getId().toString() : null)
                .queryName(execution.getQuery() != null ? execution.getQuery().getName() : "Ad-hoc Query")
                .sqlQuery(execution.getSqlQuery())
                .status(execution.getExecutionStatus())
                .executionTimeMs(execution.getExecutionTimeMs())
                .rowsReturned(execution.getRowsReturned())
                .errorMessage(execution.getErrorMessage())
                .executedAt(execution.getExecutedAt())
                .build();
    }
}