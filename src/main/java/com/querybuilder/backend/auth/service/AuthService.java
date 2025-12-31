package com.querybuilder.backend.auth.service;

import com.querybuilder.backend.auth.dto.AuthResponse;
import com.querybuilder.backend.auth.dto.LoginRequest;
import com.querybuilder.backend.auth.dto.RegisterRequest;
import com.querybuilder.backend.auth.model.RefreshToken;
import com.querybuilder.backend.auth.model.Role;
import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.auth.model.UserPreferences;
import com.querybuilder.backend.auth.repository.UserPreferencesRepository;
import com.querybuilder.backend.auth.repository.UserRepository;
import com.querybuilder.backend.security.JwtUtil;
import com.querybuilder.backend.shared.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations (register, login, refresh token)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", request.getEmail());
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        // Create default preferences for user
        UserPreferences preferences = UserPreferences.builder()
                .user(user)
                .theme("light")
                .queryTimeout(30)
                .resultLimit(1000)
                .build();

        userPreferencesRepository.save(preferences);
        log.info("User preferences created for user ID: {}", user.getId());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Tokens generated for user: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    /**
     * Login user
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting to login user: {}", request.getEmail());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        log.info("User authenticated successfully: {}", user.getEmail());

        // Check if user is active
        if (!user.getActive()) {
            log.warn("Login failed: User account is inactive - {}", user.getEmail());
            throw new RuntimeException("User account is inactive");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login successful for user: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        log.info("Attempting to refresh token");

        // Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = jwtUtil.generateAccessToken(user);

        log.info("Token refreshed successfully for user: {}", user.getEmail());

        return buildAuthResponse(user, accessToken, refreshTokenString);
    }

    /**
     * Logout user (delete refresh token)
     */
    @Transactional
    public void logout(String refreshToken) {
        log.info("Attempting to logout user");
        refreshTokenService.deleteRefreshToken(refreshToken);
        log.info("User logged out successfully");
    }

    /**
     * Build AuthResponse from user and tokens
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId().toString())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userInfo)
                .build();
    }
}