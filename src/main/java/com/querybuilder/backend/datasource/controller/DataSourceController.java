package com.querybuilder.backend.datasource.controller;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.dto.*;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.repository.DataSourceRepository;
import com.querybuilder.backend.datasource.service.DataSourceService;
import com.querybuilder.backend.datasource.service.SchemaDiscoveryService;
import com.querybuilder.backend.shared.dto.ApiResponse;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for data source management
 */
@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
@Slf4j
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final DataSourceRepository dataSourceRepository;

    /**
     * Get all data sources for authenticated user
     * GET /api/datasources
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DataSourceResponse>>> getAllDataSources(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/datasources - User: {}", user.getEmail());

        List<DataSourceResponse> dataSources = dataSourceService.getAllDataSources(user);

        return ResponseEntity.ok(
                ApiResponse.success("Data sources retrieved successfully", dataSources)
        );
    }

    /**
     * Get data source by ID
     * GET /api/datasources/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DataSourceResponse>> getDataSourceById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/datasources/{} - User: {}", id, user.getEmail());

        DataSourceResponse dataSource = dataSourceService.getDataSourceById(id, user);

        return ResponseEntity.ok(
                ApiResponse.success("Data source retrieved successfully", dataSource)
        );
    }

    /**
     * Create new data source
     * POST /api/datasources
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DataSourceResponse>> createDataSource(
            @Valid @RequestBody CreateDataSourceRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/datasources - User: {}", user.getEmail());

        DataSourceResponse dataSource = dataSourceService.createDataSource(request, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Data source created successfully", dataSource));
    }

    /**
     * Update data source
     * PUT /api/datasources/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DataSourceResponse>> updateDataSource(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDataSourceRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("PUT /api/datasources/{} - User: {}", id, user.getEmail());

        DataSourceResponse dataSource = dataSourceService.updateDataSource(id, request, user);

        return ResponseEntity.ok(
                ApiResponse.success("Data source updated successfully", dataSource)
        );
    }

    /**
     * Delete data source
     * DELETE /api/datasources/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDataSource(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("DELETE /api/datasources/{} - User: {}", id, user.getEmail());

        dataSourceService.deleteDataSource(id, user);

        return ResponseEntity.ok(
                ApiResponse.success("Data source deleted successfully", null)
        );
    }

    /**
     * Test data source connection
     * POST /api/datasources/{id}/test
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<ConnectionTestResponse>> testConnection(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/datasources/{}/test - User: {}", id, user.getEmail());

        ConnectionTestResponse result = dataSourceService.testConnection(id, user);

        return ResponseEntity.ok(
                ApiResponse.success("Connection test completed", result)
        );
    }

    /**
     * Get database schema
     * GET /api/datasources/{id}/schema
     */
    @GetMapping("/{id}/schema")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchema(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/datasources/{}/schema - User: {}", id, user.getEmail());

        DataSource dataSource = dataSourceRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", "id", id));

        Map<String, Object> schema = schemaDiscoveryService.getSchema(dataSource);

        return ResponseEntity.ok(
                ApiResponse.success("Schema retrieved successfully", schema)
        );
    }

    /**
     * Clear schema cache
     * DELETE /api/datasources/{id}/schema/cache
     */
    @DeleteMapping("/{id}/schema/cache")
    public ResponseEntity<ApiResponse<Void>> clearSchemaCache(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("DELETE /api/datasources/{}/schema/cache - User: {}", id, user.getEmail());

        DataSource dataSource = dataSourceRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", "id", id));

        schemaDiscoveryService.clearCache(dataSource);

        return ResponseEntity.ok(
                ApiResponse.success("Schema cache cleared successfully", null)
        );
    }
}