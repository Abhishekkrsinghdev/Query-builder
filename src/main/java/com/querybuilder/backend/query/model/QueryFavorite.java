package com.querybuilder.backend.query.model;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * QueryFavorite entity for user's favorite queries
 */
@Entity
@Table(
        name = "query_favorites",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_query_id", columnList = "query_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_query", columnNames = {"user_id", "query_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryFavorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;
}