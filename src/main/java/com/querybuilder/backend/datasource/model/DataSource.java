package com.querybuilder.backend.datasource.model;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.shared.model.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DataSource entity representing a database connection
 */
@Entity
@Table(
        name = "data_sources",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_database_type", columnList = "database_type"),
                @Index(name = "idx_deleted", columnList = "deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSource extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "database_type", nullable = false)
    @NotNull(message = "Database type is required")
    private DatabaseType databaseType;

    @Column(name = "host", nullable = false)
    @NotBlank(message = "Host is required")
    private String host;

    @Column(name = "port", nullable = false)
    @NotNull(message = "Port is required")
    private Integer port;

    @Column(name = "database_name", nullable = false, length = 100)
    @NotBlank(message = "Database name is required")
    private String databaseName;

    @Column(name = "username", nullable = false, length = 100)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(name = "password", nullable = false, length = 500)
    @NotBlank(message = "Password is required")
    private String password;  // Will be encrypted

    @Column(name = "ssl_enabled", nullable = false)
    @Builder.Default
    private Boolean sslEnabled = false;

    @Column(name = "connection_params", columnDefinition = "TEXT")
    private String connectionParams;  // JSON string for additional params

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.ACTIVE;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;
}