package com.querybuilder.backend.ai.controller;

import com.querybuilder.backend.ai.dto.*;
import com.querybuilder.backend.ai.service.AIService;
import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for AI-powered features
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIService aiService;

    /**
     * Convert natural language to SQL
     * POST /api/ai/nl-to-sql
     */
    @PostMapping("/nl-to-sql")
    public ResponseEntity<ApiResponse<AIResponse>> naturalLanguageToSQL(
            @Valid @RequestBody NaturalLanguageRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/ai/nl-to-sql - User: {}", user.getEmail());

        AIResponse response = aiService.naturalLanguageToSQL(
                request.getPrompt(),
                request.getDataSourceId(),
                user
        );

        return ResponseEntity.ok(
                ApiResponse.success("SQL generated successfully", response)
        );
    }

    /**
     * Optimize SQL query
     * POST /api/ai/optimize
     */
    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<OptimizationSuggestion>> optimizeQuery(
            @Valid @RequestBody OptimizeQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/ai/optimize - User: {}", user.getEmail());

        OptimizationSuggestion suggestion = aiService.optimizeQuery(
                request.getSqlQuery(),
                request.getDataSourceId(),
                user
        );

        return ResponseEntity.ok(
                ApiResponse.success("Query optimization completed", suggestion)
        );
    }

    /**
     * Explain SQL query
     * POST /api/ai/explain
     */
    @PostMapping("/explain")
    public ResponseEntity<ApiResponse<AIResponse>> explainQuery(
            @Valid @RequestBody ExplainQueryRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("POST /api/ai/explain - User: {}", user.getEmail());

        AIResponse response = aiService.explainQuery(request.getSqlQuery(), user);

        return ResponseEntity.ok(
                ApiResponse.success("Query explanation generated", response)
        );
    }

    /**
     * Get AI usage statistics
     * GET /api/ai/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAIStats(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/ai/stats - User: {}", user.getEmail());

        Map<String, Object> stats = aiService.getUserAIStats(user);

        return ResponseEntity.ok(
                ApiResponse.success("AI statistics retrieved", stats)
        );
    }
}