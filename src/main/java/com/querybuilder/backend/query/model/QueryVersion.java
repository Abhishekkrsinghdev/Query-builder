package com.querybuilder.backend.query.model;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * QueryVersion entity for tracking query changes
 */
@Entity
@Table(
        name = "query_versions",
        indexes = {
                @Index(name = "idx_query_id", columnList = "query_id"),
                @Index(name = "idx_version", columnList = "query_id,version_number")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_query_version", columnNames = {"query_id", "version_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "sql_query", columnDefinition = "TEXT", nullable = false)
    private String sqlQuery;

    @Column(name = "visual_config", columnDefinition = "LONGTEXT")
    private String visualConfig;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}