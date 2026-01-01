package com.querybuilder.backend.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryVersionResponse {

    private String id;
    private Integer versionNumber;
    private String sqlQuery;
    private String visualConfig;
    private String changeSummary;
    private String createdByName;
    private LocalDateTime createdAt;
}