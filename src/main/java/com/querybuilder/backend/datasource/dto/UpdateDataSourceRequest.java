package com.querybuilder.backend.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDataSourceRequest {

    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private Boolean sslEnabled;
    private String connectionParams;
}