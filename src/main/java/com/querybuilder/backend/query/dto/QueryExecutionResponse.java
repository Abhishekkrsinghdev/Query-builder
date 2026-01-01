package com.querybuilder.backend.query.dto;

import com.querybuilder.backend.query.model.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryExecutionResponse {

    private String executionId;
    private ExecutionStatus status;
    private Integer executionTimeMs;
    private Integer rowsReturned;
    private String errorMessage;
    private LocalDateTime executedAt;

    private List<ColumnInfo> columns;
    private List<Map<String, Object>> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        private String name;
        private String type;
        private Boolean nullable;
    }
}