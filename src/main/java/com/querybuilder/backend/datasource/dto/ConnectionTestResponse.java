package com.querybuilder.backend.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResponse {

    private Boolean success;
    private String message;
    private Long responseTimeMs;  // How long the connection test took
}