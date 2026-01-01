package com.querybuilder.backend.query.repository;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QueryExecutionRepository extends JpaRepository<QueryExecution, UUID> {

    /**
     * Find all executions for a user (paginated)
     */
    Page<QueryExecution> findByUserOrderByExecutedAtDesc(User user, Pageable pageable);

    /**
     * Find executions for a specific query
     */
    List<QueryExecution> findByQueryOrderByExecutedAtDesc(Query query);

    /**
     * Find recent executions for a user
     */
    List<QueryExecution> findTop10ByUserOrderByExecutedAtDesc(User user);

    /**
     * Count total executions for a user
     */
    long countByUser(User user);

    /**
     * Find slow queries (execution time > threshold)
     */
    @org.springframework.data.jpa.repository.Query("SELECT qe FROM QueryExecution qe WHERE qe.executionTimeMs > :thresholdMs " +
            "ORDER BY qe.executionTimeMs DESC")
    List<QueryExecution> findSlowQueries(Integer thresholdMs);

    /**
     * Count executions in date range
     */
    long countByUserAndExecutedAtBetween(User user, LocalDateTime start, LocalDateTime end);
}