package com.querybuilder.backend.auth.model;

import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "user_preferences",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "theme", length = 20)
    @Builder.Default
    private String theme = "light";

    @Column(name = "default_datasource_id", columnDefinition = "BINARY(16)")
    private UUID defaultDatasourceId;

    @Column(name = "query_timeout")
    @Builder.Default
    private Integer queryTimeout = 30;

    @Column(name = "result_limit")
    @Builder.Default
    private Integer resultLimit = 1000;
}