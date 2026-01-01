package com.querybuilder.backend.query.model;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QueryExecution entity for tracking query executions
 */
@Entity
@Table(
        name = "query_executions",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_query_id", columnList = "query_id"),
                @Index(name = "idx_datasource_id", columnList = "datasource_id"),
                @Index(name = "idx_executed_at", columnList = "executed_at"),
                @Index(name = "idx_status", columnList = "execution_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id")
    private Query query;  // Can be null for ad-hoc queries

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    private DataSource dataSource;

    @Column(name = "sql_query", columnDefinition = "TEXT", nullable = false)
    private String sqlQuery;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    private ExecutionStatus executionStatus;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "rows_returned")
    private Integer rowsReturned;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();
}