package com.querybuilder.backend.query.model;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.shared.model.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Query entity representing a saved query
 */
@Entity
@Table(
        name = "queries",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_datasource_id", columnList = "datasource_id"),
                @Index(name = "idx_is_public", columnList = "is_public"),
                @Index(name = "idx_category", columnList = "category"),
                @Index(name = "idx_is_template", columnList = "is_template"),
                @Index(name = "idx_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Query extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    private DataSource dataSource;

    @Column(name = "name", nullable = false, length = 200)
    @NotBlank(message = "Query name is required")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sql_query", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "SQL query is required")
    private String sqlQuery;

    @Column(name = "visual_config", columnDefinition = "LONGTEXT")
    private String visualConfig;  // JSON: React Flow canvas state

    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private Boolean isTemplate = false;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags;  // JSON array of tags

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;
}