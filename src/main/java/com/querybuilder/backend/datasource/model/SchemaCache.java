package com.querybuilder.backend.datasource.model;

import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SchemaCache entity for caching database schemas
 */
@Entity
@Table(
        name = "schema_cache",
        indexes = {
                @Index(name = "idx_datasource_id", columnList = "datasource_id"),
                @Index(name = "idx_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaCache extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    private DataSource dataSource;

    @Column(name = "schema_data", columnDefinition = "LONGTEXT", nullable = false)
    private String schemaData;  // JSON string with tables, columns, relationships

    @Column(name = "cached_at", nullable = false)
    @Builder.Default
    private LocalDateTime cachedAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Check if cache is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}