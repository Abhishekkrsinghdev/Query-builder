package com.querybuilder.backend.query.dto;

import com.querybuilder.backend.query.model.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryHistoryResponse {

    private String id;
    private String queryId;
    private String queryName;
    private String sqlQuery;
    private ExecutionStatus status;
    private Integer executionTimeMs;
    private Integer rowsReturned;
    private String errorMessage;
    private LocalDateTime executedAt;
}