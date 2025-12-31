package com.querybuilder.backend.auth.service;

import com.querybuilder.backend.auth.model.RefreshToken;
import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.auth.repository.RefreshTokenRepository;
import com.querybuilder.backend.security.JwtUtil;
import com.querybuilder.backend.shared.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing refresh tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    /**
     * Create and save a new refresh token for user
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Delete any existing refresh tokens for this user (single device login)
        // Comment this line if you want multi-device login support
        refreshTokenRepository.deleteByUser(user);

        // Generate refresh token
        String token = jwtUtil.generateRefreshToken(user);

        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        // Create and save refresh token entity
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify and return refresh token
     */
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        return refreshToken;
    }

    /**
     * Delete refresh token (logout)
     */
    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    /**
     * Delete all refresh tokens for a user (logout from all devices)
     */
    @Transactional
    public void deleteAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Cleanup expired tokens (scheduled job can call this)
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
        log.info("Deleted expired refresh tokens");
    }
}