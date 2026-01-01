package com.querybuilder.backend.query.repository;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QueryShareRepository extends JpaRepository<QueryShare, UUID> {

    /**
     * Find all shares for a query
     */
    List<QueryShare> findByQuery(Query query);

    /**
     * Find queries shared with a user
     */
    List<QueryShare> findBySharedWithUser(User user);

    /**
     * Check if query is shared with user
     */
    boolean existsByQueryAndSharedWithUser(Query query, User user);

    /**
     * Delete share
     */
    void deleteByQueryAndSharedWithUser(Query query, User user);
}