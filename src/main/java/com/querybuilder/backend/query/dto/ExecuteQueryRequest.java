package com.querybuilder.backend.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteQueryRequest {

    @NotBlank(message = "SQL query is required")
    private String sqlQuery;

    @NotNull(message = "Data source ID is required")
    private UUID dataSourceId;

    private Map<String, Object> parameters;  // Query parameters

    private Integer limit = 1000;  // Default row limit

    private Integer timeout = 30;  // Default timeout in seconds
}