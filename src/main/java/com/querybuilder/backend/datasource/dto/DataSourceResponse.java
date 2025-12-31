package com.querybuilder.backend.datasource.dto;

import com.querybuilder.backend.datasource.model.ConnectionStatus;
import com.querybuilder.backend.datasource.model.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceResponse {

    private String id;
    private String name;
    private DatabaseType databaseType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    // Note: password is NOT included in response for security
    private Boolean sslEnabled;
    private ConnectionStatus status;
    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}