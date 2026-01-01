package com.querybuilder.backend.query.model;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QueryShare entity for sharing queries with other users
 */
@Entity
@Table(
        name = "query_shares",
        indexes = {
                @Index(name = "idx_query_id", columnList = "query_id"),
                @Index(name = "idx_shared_with", columnList = "shared_with_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryShare extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private Query query;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id")
    private User sharedWithUser;  // NULL = public to all

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    @Builder.Default
    private SharePermission permission = SharePermission.VIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by", nullable = false)
    private User sharedBy;

    @Column(name = "shared_at", nullable = false)
    @Builder.Default
    private LocalDateTime sharedAt = LocalDateTime.now();
}