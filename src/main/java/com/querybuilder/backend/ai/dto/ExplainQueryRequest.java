package com.querybuilder.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplainQueryRequest {

    @NotBlank(message = "SQL query is required")
    private String sqlQuery;
}