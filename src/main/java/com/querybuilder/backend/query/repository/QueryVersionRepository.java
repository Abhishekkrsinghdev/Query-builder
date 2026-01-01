package com.querybuilder.backend.query.repository;

import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryVersionRepository extends JpaRepository<QueryVersion, UUID> {

    /**
     * Find all versions for a query
     */
    List<QueryVersion> findByQueryOrderByVersionNumberDesc(Query query);

    /**
     * Find specific version of a query
     */
    Optional<QueryVersion> findByQueryAndVersionNumber(Query query, Integer versionNumber);

    /**
     * Get latest version number for a query
     */
    Optional<QueryVersion> findFirstByQueryOrderByVersionNumberDesc(Query query);
}