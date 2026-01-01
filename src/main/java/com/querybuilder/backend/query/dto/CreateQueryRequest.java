package com.querybuilder.backend.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueryRequest {

    @NotBlank(message = "Query name is required")
    private String name;

    private String description;

    @NotBlank(message = "SQL query is required")
    private String sqlQuery;

    @NotNull(message = "Data source ID is required")
    private UUID dataSourceId;

    private String visualConfig;  // JSON string

    private Boolean isTemplate = false;

    private Boolean isPublic = false;

    private String category;

    private List<String> tags;
}