package com.querybuilder.backend.auth.repository;

import com.querybuilder.backend.auth.model.RefreshToken;
import com.querybuilder.backend.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token string
     * @param token the refresh token
     * @return Optional containing RefreshToken if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh tokens for a user (logout from all devices)
     * @param user the user
     */
    void deleteByUser(User user);

    /**
     * Delete a specific refresh token (logout from single device)
     * @param token the refresh token
     */
    void deleteByToken(String token);

    /**
     * Delete all expired tokens (cleanup job)
     * @param now current timestamp
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteAllExpiredTokens(LocalDateTime now);
}