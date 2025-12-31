package com.querybuilder.backend.datasource.repository;

import com.querybuilder.backend.datasource.model.DataSource;
import com.querybuilder.backend.datasource.model.SchemaCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchemaCacheRepository extends JpaRepository<SchemaCache, UUID> {

    /**
     * Find non-expired cache for data source
     */
    @Query("SELECT sc FROM SchemaCache sc WHERE sc.dataSource = :dataSource AND sc.expiresAt > :now")
    Optional<SchemaCache> findValidCacheByDataSource(DataSource dataSource, LocalDateTime now);

    /**
     * Delete cache for data source
     */
    void deleteByDataSource(DataSource dataSource);

    /**
     * Delete all expired caches
     */
    @Modifying
    @Query("DELETE FROM SchemaCache sc WHERE sc.expiresAt < :now")
    void deleteAllExpiredCaches(LocalDateTime now);
}