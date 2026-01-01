package com.querybuilder.backend.ai.repository;

import com.querybuilder.backend.ai.model.AIInteraction;
import com.querybuilder.backend.ai.model.InteractionType;
import com.querybuilder.backend.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AIInteractionRepository extends JpaRepository<AIInteraction, UUID> {

    /**
     * Find recent interactions for user
     */
    List<AIInteraction> findTop10ByUserOrderByCreatedAtDesc(User user);

    /**
     * Count interactions by type for user
     */
    long countByUserAndInteractionType(User user, InteractionType type);

    /**
     * Find interactions in date range
     */
    List<AIInteraction> findByUserAndCreatedAtBetween(
            User user, LocalDateTime start, LocalDateTime end);

    /**
     * Get total tokens used by user
     */
    @Query("SELECT SUM(ai.tokensUsed) FROM AIInteraction ai WHERE ai.user = :user")
    Long getTotalTokensUsed(User user);
}