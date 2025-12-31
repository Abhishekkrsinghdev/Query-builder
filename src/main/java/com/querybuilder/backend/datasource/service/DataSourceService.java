package com.querybuilder.backend.datasource.service;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.dto.*;
import com.querybuilder.backend.datasource.model.ConnectionStatus;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.repository.DataSourceRepository;
import com.querybuilder.backend.datasource.repository.SchemaCacheRepository;
import com.querybuilder.backend.shared.exception.ResourceAlreadyExistsException;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import com.querybuilder.backend.shared.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing data sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceService {

    private final DataSourceRepository dataSourceRepository;
    private final SchemaCacheRepository schemaCacheRepository;
    private final EncryptionUtil encryptionUtil;
    private final ConnectionTestService connectionTestService;

    /**
     * Get all data sources for a user
     */
    @Transactional(readOnly = true)
    public List<DataSourceResponse> getAllDataSources(User user) {
        log.info("Fetching all data sources for user: {}", user.getEmail());

        List<DataSource> dataSources = dataSourceRepository.findByUserAndDeletedFalse(user);

        return dataSources.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get data source by ID
     */
    @Transactional(readOnly = true)
    public DataSourceResponse getDataSourceById(UUID id, User user) {
        log.info("Fetching data source with ID: {} for user: {}", id, user.getEmail());

        DataSource dataSource = findDataSourceByIdAndUser(id, user);
        return mapToResponse(dataSource);
    }

    /**
     * Create new data source
     */
    @Transactional
    public DataSourceResponse createDataSource(CreateDataSourceRequest request, User user) {
        log.info("Creating data source '{}' for user: {}", request.getName(), user.getEmail());

        // Check if name already exists for this user
        if (dataSourceRepository.existsByUserAndNameAndDeletedFalse(user, request.getName())) {
            throw new ResourceAlreadyExistsException("DataSource", "name", request.getName());
        }

        // Encrypt password
        String encryptedPassword = encryptionUtil.encrypt(request.getPassword());

        // Create data source
        DataSource dataSource = DataSource.builder()
                .user(user)
                .name(request.getName())
                .databaseType(request.getDatabaseType())
                .host(request.getHost())
                .port(request.getPort())
                .databaseName(request.getDatabaseName())
                .username(request.getUsername())
                .password(encryptedPassword)
                .sslEnabled(request.getSslEnabled())
                .connectionParams(request.getConnectionParams())
                .status(ConnectionStatus.ACTIVE)
                .build();

        dataSource = dataSourceRepository.save(dataSource);
        log.info("Data source created with ID: {}", dataSource.getId());

        return mapToResponse(dataSource);
    }

    /**
     * Update data source
     */
    @Transactional
    public DataSourceResponse updateDataSource(UUID id, UpdateDataSourceRequest request, User user) {
        log.info("Updating data source with ID: {} for user: {}", id, user.getEmail());

        DataSource dataSource = findDataSourceByIdAndUser(id, user);

        // Update fields if provided
        if (request.getName() != null) {
            // Check if new name conflicts with existing
            if (!dataSource.getName().equals(request.getName()) &&
                    dataSourceRepository.existsByUserAndNameAndDeletedFalse(user, request.getName())) {
                throw new ResourceAlreadyExistsException("DataSource", "name", request.getName());
            }
            dataSource.setName(request.getName());
        }

        if (request.getHost() != null) {
            dataSource.setHost(request.getHost());
        }

        if (request.getPort() != null) {
            dataSource.setPort(request.getPort());
        }

        if (request.getDatabaseName() != null) {
            dataSource.setDatabaseName(request.getDatabaseName());
        }

        if (request.getUsername() != null) {
            dataSource.setUsername(request.getUsername());
        }

        if (request.getPassword() != null) {
            String encryptedPassword = encryptionUtil.encrypt(request.getPassword());
            dataSource.setPassword(encryptedPassword);
        }

        if (request.getSslEnabled() != null) {
            dataSource.setSslEnabled(request.getSslEnabled());
        }

        if (request.getConnectionParams() != null) {
            dataSource.setConnectionParams(request.getConnectionParams());
        }

        // Clear cached schema when connection details change
        schemaCacheRepository.deleteByDataSource(dataSource);

        dataSource = dataSourceRepository.save(dataSource);
        log.info("Data source updated: {}", dataSource.getId());

        return mapToResponse(dataSource);
    }

    /**
     * Delete data source (soft delete)
     */
    @Transactional
    public void deleteDataSource(UUID id, User user) {
        log.info("Deleting data source with ID: {} for user: {}", id, user.getEmail());

        DataSource dataSource = findDataSourceByIdAndUser(id, user);

        // Soft delete
        dataSource.markAsDeleted();
        dataSourceRepository.save(dataSource);

        // Clear cached schema
        schemaCacheRepository.deleteByDataSource(dataSource);

        log.info("Data source soft-deleted: {}", dataSource.getId());
    }

    /**
     * Test data source connection
     */
    @Transactional
    public ConnectionTestResponse testConnection(UUID id, User user) {
        log.info("Testing connection for data source ID: {}", id);

        DataSource dataSource = findDataSourceByIdAndUser(id, user);

        // Decrypt password
        String decryptedPassword = encryptionUtil.decrypt(dataSource.getPassword());

        // Test connection
        ConnectionTestResponse response = connectionTestService.testConnection(
                dataSource.getDatabaseType(),
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDatabaseName(),
                dataSource.getUsername(),
                decryptedPassword,
                dataSource.getSslEnabled()
        );

        // Update status and last tested time
        if (response.getSuccess()) {
            dataSource.setStatus(ConnectionStatus.ACTIVE);
        } else {
            dataSource.setStatus(ConnectionStatus.ERROR);
        }
        dataSource.setLastTestedAt(LocalDateTime.now());
        dataSourceRepository.save(dataSource);

        log.info("Connection test completed for data source: {} - Success: {}",
                id, response.getSuccess());

        return response;
    }

    /**
     * Helper: Find data source by ID and user
     */
    private DataSource findDataSourceByIdAndUser(UUID id, User user) {
        return dataSourceRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", "id", id));
    }

    /**
     * Helper: Map DataSource entity to response DTO
     */
    private DataSourceResponse mapToResponse(DataSource dataSource) {
        return DataSourceResponse.builder()
                .id(dataSource.getId().toString())
                .name(dataSource.getName())
                .databaseType(dataSource.getDatabaseType())
                .host(dataSource.getHost())
                .port(dataSource.getPort())
                .databaseName(dataSource.getDatabaseName())
                .username(dataSource.getUsername())
                // Password is NOT included in response
                .sslEnabled(dataSource.getSslEnabled())
                .status(dataSource.getStatus())
                .lastTestedAt(dataSource.getLastTestedAt())
                .createdAt(dataSource.getCreatedAt())
                .updatedAt(dataSource.getUpdatedAt())
                .build();
    }
}