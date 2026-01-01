package com.querybuilder.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querybuilder.backend.ai.dto.AIResponse;
import com.querybuilder.backend.ai.dto.OptimizationSuggestion;
import com.querybuilder.backend.ai.model.AIInteraction;
import com.querybuilder.backend.ai.model.InteractionType;
import com.querybuilder.backend.ai.repository.AIInteractionRepository;
import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.repository.DataSourceRepository;
import com.querybuilder.backend.datasource.service.SchemaDiscoveryService;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for AI-powered features using Spring AI + Ollama
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final ChatClient.Builder chatClientBuilder;
    private final AIInteractionRepository aiInteractionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final ObjectMapper objectMapper;

    /**
     * Convert natural language to SQL
     */
    @Transactional
    public AIResponse naturalLanguageToSQL(String prompt, UUID dataSourceId, User user) {
        log.info("Converting NL to SQL for user: {}", user.getEmail());

        long startTime = System.currentTimeMillis();

        // Get data source and schema
        DataSource dataSource = dataSourceRepository
                .findByIdAndUserAndDeletedFalse(dataSourceId, user)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", "id", dataSourceId));

        Map<String, Object> schema = schemaDiscoveryService.getSchema(dataSource);

        // Build prompt with schema context
        String systemPrompt = buildNLToSQLPrompt(schema, dataSource.getDatabaseType().name());

        // Call AI
        ChatClient chatClient = chatClientBuilder.build();
        String sqlQuery = chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;

        // Clean SQL (remove markdown formatting if present)
        sqlQuery = cleanSQLResponse(sqlQuery);

        // Save interaction
        saveInteraction(user, dataSource, InteractionType.NL_TO_SQL,
                prompt, sqlQuery, responseTime);

        log.info("NL to SQL conversion completed in {}ms", responseTime);

        return AIResponse.builder()
                .result(sqlQuery)
                .explanation("SQL query generated from natural language")
                .responseTimeMs(responseTime)
                .build();
    }

    /**
     * Optimize SQL query
     */
    @Transactional
    public OptimizationSuggestion optimizeQuery(String sqlQuery, UUID dataSourceId, User user) {
        log.info("Optimizing query for user: {}", user.getEmail());

        long startTime = System.currentTimeMillis();

        DataSource dataSource = dataSourceRepository
                .findByIdAndUserAndDeletedFalse(dataSourceId, user)
                .orElseThrow(() -> new ResourceNotFoundException("DataSource", "id", dataSourceId));

        Map<String, Object> schema = schemaDiscoveryService.getSchema(dataSource);

        String systemPrompt = buildOptimizationPrompt(schema, dataSource.getDatabaseType().name());

        ChatClient chatClient = chatClientBuilder.build();
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user("Analyze and optimize this SQL query:\n\n" + sqlQuery)
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;

        // Parse AI response into structured suggestions
        OptimizationSuggestion suggestion = parseOptimizationResponse(response);
        suggestion.setResponseTimeMs(responseTime);

        // Save interaction
        saveInteraction(user, dataSource, InteractionType.OPTIMIZATION,
                sqlQuery, response, responseTime);

        log.info("Query optimization completed in {}ms", responseTime);

        return suggestion;
    }

    /**
     * Explain SQL query in plain English
     */
    @Transactional
    public AIResponse explainQuery(String sqlQuery, User user) {
        log.info("Explaining query for user: {}", user.getEmail());

        long startTime = System.currentTimeMillis();

        String systemPrompt = """
            You are a SQL expert. Explain the given SQL query in simple, plain English.
            Break down the query step by step, explaining what each part does.
            Make it understandable for someone who doesn't know SQL well.
            """;

        ChatClient chatClient = chatClientBuilder.build();
        String explanation = chatClient.prompt()
                .system(systemPrompt)
                .user("Explain this SQL query:\n\n" + sqlQuery)
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;

        // Save interaction
        saveInteraction(user, null, InteractionType.EXPLANATION,
                sqlQuery, explanation, responseTime);

        log.info("Query explanation completed in {}ms", responseTime);

        return AIResponse.builder()
                .result(explanation)
                .responseTimeMs(responseTime)
                .build();
    }

    /**
     * Get AI usage statistics for user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserAIStats(User user) {
        long nlToSqlCount = aiInteractionRepository.countByUserAndInteractionType(
                user, InteractionType.NL_TO_SQL);
        long optimizationCount = aiInteractionRepository.countByUserAndInteractionType(
                user, InteractionType.OPTIMIZATION);
        long explanationCount = aiInteractionRepository.countByUserAndInteractionType(
                user, InteractionType.EXPLANATION);

        Long totalTokens = aiInteractionRepository.getTotalTokensUsed(user);

        return Map.of(
                "nlToSqlCount", nlToSqlCount,
                "optimizationCount", optimizationCount,
                "explanationCount", explanationCount,
                "totalTokensUsed", totalTokens != null ? totalTokens : 0
        );
    }

    /**
     * Build NL-to-SQL prompt with schema context
     */
    private String buildNLToSQLPrompt(Map<String, Object> schema, String databaseType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a SQL expert. Convert natural language queries to SQL.\n\n");
        prompt.append("Database Type: ").append(databaseType).append("\n\n");
        prompt.append("Available Tables and Columns:\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) schema.get("tables");

        if (tables != null) {
            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("name");
                prompt.append("\nTable: ").append(tableName).append("\n");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> columns =
                        (List<Map<String, Object>>) table.get("columns");

                if (columns != null) {
                    for (Map<String, Object> column : columns) {
                        prompt.append("  - ").append(column.get("name"))
                                .append(" (").append(column.get("type")).append(")\n");
                    }
                }
            }
        }

        prompt.append("\nRules:\n");
        prompt.append("1. Return ONLY the SQL query, no explanations\n");
        prompt.append("2. Use proper SQL syntax for ").append(databaseType).append("\n");
        prompt.append("3. Use table and column names EXACTLY as shown above\n");
        prompt.append("4. Do not use SELECT * unless explicitly requested\n");
        prompt.append("5. Add appropriate WHERE clauses for safety\n");

        return prompt.toString();
    }

    /**
     * Build optimization prompt
     */
    private String buildOptimizationPrompt(Map<String, Object> schema, String databaseType) {
        return """
            You are a SQL performance expert. Analyze the given SQL query and provide optimization suggestions.
            
            Focus on:
            1. Missing indexes
            2. Inefficient JOINs
            3. Subquery optimization
            4. SELECT * usage
            5. Missing WHERE clauses on large tables
            
            Provide:
            - Optimized version of the query
            - List of specific suggestions with severity (HIGH/MEDIUM/LOW)
            - Estimated performance improvement
            
            Format your response as:
            OPTIMIZED QUERY:
            [optimized SQL here]
            
            SUGGESTIONS:
            1. [Type: INDEX] [Severity: HIGH] Description...
            2. [Type: REWRITE] [Severity: MEDIUM] Description...
            
            Database Type: """ + databaseType;
    }

    /**
     * Clean SQL response (remove markdown formatting)
     */
    private String cleanSQLResponse(String sql) {
        // Remove ```sql and ``` markers
        sql = sql.replaceAll("```sql\\n?", "");
        sql = sql.replaceAll("```\\n?", "");
        return sql.trim();
    }

    /**
     * Parse optimization response into structured format
     */
    private OptimizationSuggestion parseOptimizationResponse(String response) {
        List<OptimizationSuggestion.Suggestion> suggestions = new ArrayList<>();

        String[] parts = response.split("SUGGESTIONS:");
        String optimizedQuery = "";
        String explanation = response;

        if (parts.length > 0) {
            String queryPart = parts[0].replace("OPTIMIZED QUERY:", "").trim();
            optimizedQuery = cleanSQLResponse(queryPart);
        }

        if (parts.length > 1) {
            String suggestionsPart = parts[1].trim();
            String[] lines = suggestionsPart.split("\\n");

            for (String line : lines) {
                if (line.matches("^\\d+\\..*")) {
                    OptimizationSuggestion.Suggestion suggestion = parseSuggestionLine(line);
                    if (suggestion != null) {
                        suggestions.add(suggestion);
                    }
                }
            }
        }

        return OptimizationSuggestion.builder()
                .optimizedQuery(optimizedQuery)
                .suggestions(suggestions)
                .explanation(explanation)
                .build();
    }

    /**
     * Parse single suggestion line
     */
    private OptimizationSuggestion.Suggestion parseSuggestionLine(String line) {
        try {
            String type = "GENERAL";
            String severity = "MEDIUM";

            if (line.contains("[Type:")) {
                type = line.substring(line.indexOf("[Type:") + 6, line.indexOf("]", line.indexOf("[Type:"))).trim();
            }

            if (line.contains("[Severity:")) {
                severity = line.substring(line.indexOf("[Severity:") + 10,
                        line.indexOf("]", line.indexOf("[Severity:"))).trim();
            }

            return OptimizationSuggestion.Suggestion.builder()
                    .type(type)
                    .severity(severity)
                    .description(line)
                    .recommendation("See description")
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Save AI interaction to database
     */
    private void saveInteraction(User user, DataSource dataSource, InteractionType type,
                                 String input, String output, long responseTime) {
        AIInteraction interaction = AIInteraction.builder()
                .user(user)
                .dataSource(dataSource)
                .interactionType(type)
                .inputText(input)
                .outputText(output)
                .responseTimeMs((int) responseTime)
                .build();

        aiInteractionRepository.save(interaction);
    }
}