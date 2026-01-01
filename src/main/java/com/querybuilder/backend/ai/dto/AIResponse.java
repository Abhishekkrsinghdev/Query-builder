package com.querybuilder.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {

    private String result;
    private String explanation;
    private Double confidence;
    private Integer tokensUsed;
    private Long responseTimeMs;
}