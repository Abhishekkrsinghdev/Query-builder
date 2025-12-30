package com.querybuilder.backend.auth.repository;

import com.querybuilder.backend.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Spring Data JPA automatically implements these methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (used for login)
     * @param email user's email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists (used during registration)
     * @param email email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find active user by email
     * @param email user's email
     * @param active active status
     * @return Optional containing user if found
     */
    Optional<User> findByEmailAndActive(String email, Boolean active);
}