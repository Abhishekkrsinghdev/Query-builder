package com.querybuilder.backend.datasource.dto;

import com.querybuilder.backend.datasource.model.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDataSourceRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Database type is required")
    private DatabaseType databaseType;

    @NotBlank(message = "Host is required")
    private String host;

    @NotNull(message = "Port is required")
    private Integer port;

    @NotBlank(message = "Database name is required")
    private String databaseName;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Boolean sslEnabled = false;

    private String connectionParams;  // JSON string
}