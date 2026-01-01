package com.querybuilder.backend.query.model;

import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * QueryParameter entity for parameterized queries
 */
@Entity
@Table(
        name = "query_parameters",
        indexes = {
                @Index(name = "idx_query_id", columnList = "query_id"),
                @Index(name = "idx_display_order", columnList = "display_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryParameter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @Column(name = "param_name", nullable = false, length = 100)
    private String paramName;

    @Enumerated(EnumType.STRING)
    @Column(name = "param_type", nullable = false)
    private ParameterType paramType;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "validation_rule", length = 500)
    private String validationRule;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}