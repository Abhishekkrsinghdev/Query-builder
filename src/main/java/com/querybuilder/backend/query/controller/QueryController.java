package com.querybuilder.backend.query.controller;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.query.dto.*;
import com.querybuilder.backend.query.service.QueryExecutionService;
import com.querybuilder.backend.query.service.QueryFavoriteService;
import com.querybuilder.backend.query.service.QueryService;
import com.querybuilder.backend.query.service.QueryShareService;
import com.querybuilder.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final QueryService queryService;
    private final QueryExecutionService executionService;
    private final QueryFavoriteService favoriteService;
    private final QueryShareService shareService;

    // 1. SPECIFIC GET PATHS FIRST (Order matters!)
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<QueryResponse>>> getFavorites(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/favorites - User: {}", user.getEmail());
        List<QueryResponse> favorites = favoriteService.getFavorites(user);
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved successfully", favorites));
    }

    @GetMapping("/executions")
    public ResponseEntity<ApiResponse<Page<QueryHistoryResponse>>> getExecutionHistory(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        log.info("GET /api/queries/executions - User: {}", user.getEmail());
        Page<QueryHistoryResponse> history = executionService.getExecutionHistory(user, pageable);
        return ResponseEntity.ok(ApiResponse.success("Execution history retrieved successfully", history));
    }

    @GetMapping("/executions/recent")
    public ResponseEntity<ApiResponse<List<QueryHistoryResponse>>> getRecentExecutions(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/executions/recent - User: {}", user.getEmail());
        List<QueryHistoryResponse> recent = executionService.getRecentExecutions(user);
        return ResponseEntity.ok(ApiResponse.success("Recent executions retrieved successfully", recent));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<QueryResponse>>> getPublicQueries(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/public - User: {}", user.getEmail());
        List<QueryResponse> queries = queryService.getPublicQueries(user);
        return ResponseEntity.ok(ApiResponse.success("Public queries retrieved successfully", queries));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<QueryResponse>>> getTemplateQueries(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/templates - User: {}", user.getEmail());
        List<QueryResponse> queries = queryService.getTemplateQueries(user);
        return ResponseEntity.ok(ApiResponse.success("Template queries retrieved successfully", queries));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<QueryResponse>>> searchQueries(
            @RequestParam String q,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/search?q={} - User: {}", q, user.getEmail());
        List<QueryResponse> queries = queryService.searchQueries(q, user);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", queries));
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<ApiResponse<QueryResponse>> getQueryBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/by-slug/{} - User: {}", slug, user.getEmail());
        QueryResponse query = queryService.getQueryBySlug(slug, user);
        return ResponseEntity.ok(ApiResponse.success("Query retrieved successfully", query));
    }

    // 2. GENERIC GET PATHS LAST

    @GetMapping
    public ResponseEntity<ApiResponse<List<QueryResponse>>> getAllQueries(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries - User: {}", user.getEmail());
        List<QueryResponse> queries = queryService.getAllQueries(user);
        return ResponseEntity.ok(ApiResponse.success("Queries retrieved successfully", queries));
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<ApiResponse<QueryResponse>> getQueryById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/{} - User: {}", id, user.getEmail());
        QueryResponse query = queryService.getQueryById(id, user);
        return ResponseEntity.ok(ApiResponse.success("Query retrieved successfully", query));
    }

    // 3. POST / PUT / DELETE (Reordered similarly for consistency)

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<QueryExecutionResponse>> executeQuery(
            @Valid @RequestBody ExecuteQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/queries/execute - User: {}", user.getEmail());
        QueryExecutionResponse result = executionService.executeQuery(request, user);
        return ResponseEntity.ok(ApiResponse.success("Query executed successfully", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QueryResponse>> createQuery(
            @Valid @RequestBody CreateQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/queries - User: {}", user.getEmail());
        QueryResponse query = queryService.createQuery(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Query created successfully", query));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QueryResponse>> updateQuery(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("PUT /api/queries/{} - User: {}", id, user.getEmail());
        QueryResponse query = queryService.updateQuery(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("Query updated successfully", query));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuery(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("DELETE /api/queries/{} - User: {}", id, user.getEmail());
        queryService.deleteQuery(id, user);
        return ResponseEntity.ok(ApiResponse.success("Query deleted successfully", null));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<QueryExecutionResponse>> executeSavedQuery(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> parameters,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/queries/{}/execute - User: {}", id, user.getEmail());
        QueryExecutionResponse result = executionService.executeSavedQuery(id, user, parameters);
        return ResponseEntity.ok(ApiResponse.success("Query executed successfully", result));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<QueryVersionResponse>>> getQueryVersions(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/queries/{}/versions - User: {}", id, user.getEmail());
        List<QueryVersionResponse> versions = queryService.getQueryVersions(id, user);
        return ResponseEntity.ok(ApiResponse.success("Query versions retrieved successfully", versions));
    }

    @PostMapping("/{id}/versions/{versionNumber}/restore")
    public ResponseEntity<ApiResponse<QueryResponse>> restoreVersion(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/queries/{}/versions/{}/restore - User: {}", id, versionNumber, user.getEmail());
        QueryResponse query = queryService.restoreVersion(id, versionNumber, user);
        return ResponseEntity.ok(ApiResponse.success("Query restored to version " + versionNumber, query));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/queries/{}/favorite - User: {}", id, user.getEmail());
        favoriteService.addFavorite(id, user);
        return ResponseEntity.ok(ApiResponse.success("Query added to favorites", null));
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        log.info("DELETE /api/queries/{}/favorite - User: {}", id, user.getEmail());
        favoriteService.removeFavorite(id, user);
        return ResponseEntity.ok(ApiResponse.success("Query removed from favorites", null));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<Void>> shareQuery(
            @PathVariable UUID id,
            @Valid @RequestBody ShareQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/queries/{}/share - User: {}", id, user.getEmail());
        shareService.shareQuery(id, request, user);
        return ResponseEntity.ok(ApiResponse.success("Query shared successfully", null));
    }

    @DeleteMapping("/{id}/share/{email}")
    public ResponseEntity<ApiResponse<Void>> unshareQuery(
            @PathVariable UUID id,
            @PathVariable String email,
            @AuthenticationPrincipal User user
    ) {
        log.info("DELETE /api/queries/{}/share/{} - User: {}", id, email, user.getEmail());
        shareService.unshareQuery(id, email, user);
        return ResponseEntity.ok(ApiResponse.success("Query unshared successfully", null));
    }
}