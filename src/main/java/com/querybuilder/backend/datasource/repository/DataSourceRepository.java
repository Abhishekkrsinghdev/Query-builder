package com.querybuilder.backend.datasource.repository;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.datasource.model.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

    /**
     * Find all data sources for a user (excluding soft-deleted)
     */
    List<DataSource> findByUserAndDeletedFalse(User user);

    /**
     * Find data source by ID and user (excluding soft-deleted)
     */
    Optional<DataSource> findByIdAndUserAndDeletedFalse(UUID id, User user);

    /**
     * Check if user has data source with given name
     */
    boolean existsByUserAndNameAndDeletedFalse(User user, String name);

    /**
     * Count active data sources for a user
     */
    long countByUserAndDeletedFalse(User user);
}