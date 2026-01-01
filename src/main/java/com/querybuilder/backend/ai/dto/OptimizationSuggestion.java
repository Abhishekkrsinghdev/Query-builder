package com.querybuilder.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationSuggestion {

    private String optimizedQuery;
    private List<Suggestion> suggestions;
    private String explanation;
    private Long responseTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String type;  // INDEX, REWRITE, WARNING
        private String severity;  // HIGH, MEDIUM, LOW
        private String description;
        private String recommendation;
        private String estimatedImprovement;
    }
}