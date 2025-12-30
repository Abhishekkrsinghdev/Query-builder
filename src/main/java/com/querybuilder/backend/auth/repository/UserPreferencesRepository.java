package com.querybuilder.backend.auth.repository;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.auth.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

    /**
     * Find preferences by user
     * @param user the user
     * @return Optional containing UserPreferences if found
     */
    Optional<UserPreferences> findByUser(User user);

    /**
     * Find preferences by user ID
     * @param userId the user ID
     * @return Optional containing UserPreferences if found
     */
    Optional<UserPreferences> findByUserId(UUID userId);
}