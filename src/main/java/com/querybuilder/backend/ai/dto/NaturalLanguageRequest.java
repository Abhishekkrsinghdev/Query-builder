package com.querybuilder.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaturalLanguageRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @NotNull(message = "Data source ID is required")
    private UUID dataSourceId;

    private Boolean includeSchema = true;
}