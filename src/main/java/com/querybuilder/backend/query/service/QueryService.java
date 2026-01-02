package com.querybuilder.backend.query.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.auth.repository.UserRepository;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.repository.DataSourceRepository;
import com.querybuilder.backend.query.dto.*;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryFavorite;
import com.querybuilder.backend.query.model.QueryVersion;
import com.querybuilder.backend.query.repository.*;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import com.querybuilder.backend.shared.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final QueryRepository queryRepository;
    private final QueryVersionRepository queryVersionRepository;
    private final QueryFavoriteRepository queryFavoriteRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ObjectMapper objectMapper;
    private final SlugGenerator slugGenerator;

    /**
     * Get all queries for a user
     */
    @Transactional(readOnly = true)
    public List<QueryResponse> getAllQueries(User user) {
        log.info("Fetching all queries for user: {}", user.getEmail());

        List<Query> queries = queryRepository.findByUserAndDeletedFalseOrderByCreatedAtDesc(user);

        return queries.stream()
                .map(query -> mapToResponse(query, user))
                .collect(Collectors.toList());
    }

    /**
     * Get query by ID
     */
    @Transactional(readOnly = true)
    public QueryResponse getQueryById(UUID id, User user) {
        log.info("Fetching query with ID: {} for user: {}", id, user.getEmail());

        Query query = findQueryByIdAndUser(id, user);
        return mapToResponse(query, user);
    }

    /**
     * Create new query
     */
    @Transactional
    public QueryResponse createQuery(CreateQueryRequest request, User user) {
        log.info("Creating query '{}' for user: {}", request.getName(), user.getEmail());

        // Verify data source exists and belongs to user
        DataSource dataSource = dataSourceRepository
                .findByIdAndUserAndDeletedFalse(request.getDataSourceId(), user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DataSource", "id", request.getDataSourceId()));

        // Convert tags to JSON
        String tagsJson = null;
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                tagsJson = objectMapper.writeValueAsString(request.getTags());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize tags", e);
            }
        }

        String slug = generateUniqueSlug(request.getName());

        // Create query
        Query query = Query.builder()
                .user(user)
                .dataSource(dataSource)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .sqlQuery(request.getSqlQuery())
                .visualConfig(request.getVisualConfig())
                .isTemplate(request.getIsTemplate() != null ? request.getIsTemplate() : false)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .category(request.getCategory())
                .tags(tagsJson)
                .version(1)
                .build();

        query = queryRepository.save(query);
        log.info("Query created with ID: {}", query.getId());

        // Create initial version
        createVersion(query, user, "Initial version");

        return mapToResponse(query, user);
    }

    /**
     * Update query
     */
    @Transactional
    public QueryResponse updateQuery(UUID id, UpdateQueryRequest request, User user) {
        log.info("Updating query with ID: {} for user: {}", id, user.getEmail());

        Query query = findQueryByIdAndUser(id, user);

        boolean hasChanges = false;

        if (request.getName() != null && !request.getName().equals(query.getName())) {
            query.setName(request.getName());
            query.setSlug(generateUniqueSlug(request.getName()));
            hasChanges = true;
        }

        if (request.getDescription() != null) {
            query.setDescription(request.getDescription());
            hasChanges = true;
        }

        if (request.getSqlQuery() != null && !request.getSqlQuery().equals(query.getSqlQuery())) {
            query.setSqlQuery(request.getSqlQuery());
            hasChanges = true;
        }

        if (request.getVisualConfig() != null) {
            query.setVisualConfig(request.getVisualConfig());
            hasChanges = true;
        }

        if (request.getIsTemplate() != null) {
            query.setIsTemplate(request.getIsTemplate());
        }

        if (request.getIsPublic() != null) {
            query.setIsPublic(request.getIsPublic());
        }

        if (request.getCategory() != null) {
            query.setCategory(request.getCategory());
        }

        if (request.getTags() != null) {
            try {
                query.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize tags", e);
            }
        }

        // Create new version if there are significant changes
        if (hasChanges) {
            query.setVersion(query.getVersion() + 1);
            createVersion(query, user, request.getChangeSummary());
        }

        query = queryRepository.save(query);
        log.info("Query updated: {}", query.getId());

        return mapToResponse(query, user);
    }

    /**
     * Delete query (soft delete)
     */
    @Transactional
    public void deleteQuery(UUID id, User user) {
        log.info("Deleting query with ID: {} for user: {}", id, user.getEmail());

        Query query = findQueryByIdAndUser(id, user);
        query.markAsDeleted();
        queryRepository.save(query);

        log.info("Query soft-deleted: {}", query.getId());
    }

    /**
     * Get public queries
     */
    @Transactional(readOnly = true)
    public List<QueryResponse> getPublicQueries(User user) {
        log.info("Fetching public queries");

        List<Query> queries = queryRepository.findByIsPublicTrueAndDeletedFalseOrderByCreatedAtDesc();

        return queries.stream()
                .map(query -> mapToResponse(query, user))
                .collect(Collectors.toList());
    }

    /**
     * Get template queries
     */
    @Transactional(readOnly = true)
    public List<QueryResponse> getTemplateQueries(User user) {
        log.info("Fetching template queries");

        List<Query> queries = queryRepository.findByIsTemplateTrueAndDeletedFalseOrderByCreatedAtDesc();

        return queries.stream()
                .map(query -> mapToResponse(query, user))
                .collect(Collectors.toList());
    }

    /**
     * Search queries
     */
    @Transactional(readOnly = true)
    public List<QueryResponse> searchQueries(String searchTerm, User user) {
        log.info("Searching queries with term: {}", searchTerm);

        List<Query> queries = queryRepository.searchQueries(searchTerm);

        // Filter to only show user's own queries or public queries
        return queries.stream()
                .filter(q -> q.getUser().equals(user) || q.getIsPublic())
                .map(query -> mapToResponse(query, user))
                .collect(Collectors.toList());
    }

    /**
     * Get query versions
     */
    @Transactional(readOnly = true)
    public List<QueryVersionResponse> getQueryVersions(UUID queryId, User user) {
        log.info("Fetching versions for query: {}", queryId);

        Query query = findQueryByIdAndUser(queryId, user);
        List<QueryVersion> versions = queryVersionRepository.findByQueryOrderByVersionNumberDesc(query);

        return versions.stream()
                .map(this::mapVersionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Restore query to specific version
     */
    @Transactional
    public QueryResponse restoreVersion(UUID queryId, Integer versionNumber, User user) {
        log.info("Restoring query {} to version {}", queryId, versionNumber);

        Query query = findQueryByIdAndUser(queryId, user);

        QueryVersion version = queryVersionRepository.findByQueryAndVersionNumber(query, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QueryVersion", "versionNumber", versionNumber));

        query.setSqlQuery(version.getSqlQuery());
        query.setVisualConfig(version.getVisualConfig());
        query.setVersion(query.getVersion() + 1);

        createVersion(query, user, "Restored to version " + versionNumber);

        query = queryRepository.save(query);
        log.info("Query restored to version {}", versionNumber);

        return mapToResponse(query, user);
    }

    /**
     * Get query by slug (NEW method)
     */
    @Transactional(readOnly = true)
    public QueryResponse getQueryBySlug(String slug, User user) {
        log.info("Fetching query with slug: {} for user: {}", slug, user.getEmail());

        Query query = queryRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "slug", slug));

        // Verify user has access
        if (!query.getUser().equals(user) && !query.getIsPublic()) {
            throw new ResourceNotFoundException("Query", "slug", slug);
        }

        return mapToResponse(query, user);
    }

    /**
     * Helper: Create query version
     */
    private void createVersion(Query query, User user, String changeSummary) {
        QueryVersion version = QueryVersion.builder()
                .query(query)
                .versionNumber(query.getVersion())
                .sqlQuery(query.getSqlQuery())
                .visualConfig(query.getVisualConfig())
                .changeSummary(changeSummary)
                .createdBy(user)
                .build();

        queryVersionRepository.save(version);
        log.debug("Created version {} for query {}", version.getVersionNumber(), query.getId());
    }

    /**
     * Helper: Find query by ID and user
     */
    private Query findQueryByIdAndUser(UUID id, User user) {
        return queryRepository.findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", id));
    }

    /**
     * Helper: Map Query to Response
     */
    private QueryResponse mapToResponse(Query query, User currentUser) {
        // Check if favorited
        boolean isFavorite = queryFavoriteRepository.existsByUserAndQuery(currentUser, query);

        // Parse tags
        List<String> tags = null;
        if (query.getTags() != null) {
            try {
                tags = objectMapper.readValue(query.getTags(), List.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse tags", e);
            }
        }

        return QueryResponse.builder()
                .id(query.getId().toString())
                .slug(query.getSlug())
                .name(query.getName())
                .description(query.getDescription())
                .sqlQuery(query.getSqlQuery())
                .visualConfig(query.getVisualConfig())
                .dataSource(QueryResponse.DataSourceInfo.builder()
                        .id(query.getDataSource().getId().toString())
                        .name(query.getDataSource().getName())
                        .databaseType(query.getDataSource().getDatabaseType().name())
                        .build())
                .owner(QueryResponse.UserInfo.builder()
                        .id(query.getUser().getId().toString())
                        .name(query.getUser().getName())
                        .email(query.getUser().getEmail())
                        .build())
                .isTemplate(query.getIsTemplate())
                .isPublic(query.getIsPublic())
                .isFavorite(isFavorite)
                .category(query.getCategory())
                .tags(tags)
                .version(query.getVersion())
                .createdAt(query.getCreatedAt())
                .updatedAt(query.getUpdatedAt())
                .build();
    }

    /**
     * Helper: Map QueryVersion to Response
     */
    private QueryVersionResponse mapVersionToResponse(QueryVersion version) {
        return QueryVersionResponse.builder()
                .id(version.getId().toString())
                .versionNumber(version.getVersionNumber())
                .sqlQuery(version.getSqlQuery())
                .visualConfig(version.getVisualConfig())
                .changeSummary(version.getChangeSummary())
                .createdByName(version.getCreatedBy().getName())
                .createdAt(version.getCreatedAt())
                .build();
    }

    /**
     * Helper: Generate unique slug
     */
    private String generateUniqueSlug(String name) {
        String slug = slugGenerator.generateSlug(name);

        // Ensure uniqueness
        while (queryRepository.existsBySlug(slug)) {
            slug = slugGenerator.regenerateSlug(slug);
        }

        return slug;
    }


}