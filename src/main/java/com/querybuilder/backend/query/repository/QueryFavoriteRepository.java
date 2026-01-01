package com.querybuilder.backend.query.repository;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryFavoriteRepository extends JpaRepository<QueryFavorite, UUID> {

    /**
     * Find all favorites for a user
     */
    List<QueryFavorite> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Check if query is favorited by user
     */
    boolean existsByUserAndQuery(User user, Query query);

    /**
     * Find favorite by user and query
     */
    Optional<QueryFavorite> findByUserAndQuery(User user, Query query);

    /**
     * Delete favorite
     */
    void deleteByUserAndQuery(User user, Query query);
}