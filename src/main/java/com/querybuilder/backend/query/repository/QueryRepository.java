package com.querybuilder.backend.query.repository;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.query.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryRepository extends JpaRepository<Query, UUID> {

    /**
     * Find all queries for a user (excluding soft-deleted)
     */
    List<Query> findByUserAndDeletedFalseOrderByCreatedAtDesc(User user);

    /**
     * Find query by ID and user (excluding soft-deleted)
     */
    Optional<Query> findByIdAndUserAndDeletedFalse(UUID id, User user);

    /**
     * Find public queries (excluding soft-deleted)
     */
    List<Query> findByIsPublicTrueAndDeletedFalseOrderByCreatedAtDesc();

    /**
     * Find template queries (excluding soft-deleted)
     */
    List<Query> findByIsTemplateTrueAndDeletedFalseOrderByCreatedAtDesc();

    /**
     * Find queries by category
     */
    List<Query> findByCategoryAndDeletedFalseOrderByCreatedAtDesc(String category);

    /**
     * Search queries by name or description
     */
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Query q WHERE q.deleted = false AND " +
            "(LOWER(q.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(q.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Query> searchQueries(String searchTerm);

    /**
     * Count queries for a user
     */
    long countByUserAndDeletedFalse(User user);

    /**
     * Find query by slug (for SEO-friendly URLs)
     */
    Optional<Query> findBySlugAndDeletedFalse(String slug);

    /**
     * Check if slug exists
     */
    boolean existsBySlug(String slug);
}