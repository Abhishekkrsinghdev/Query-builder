package com.querybuilder.backend.query.service;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.query.dto.QueryResponse;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryFavorite;
import com.querybuilder.backend.query.repository.QueryFavoriteRepository;
import com.querybuilder.backend.query.repository.QueryRepository;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing query favorites
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryFavoriteService {

    private final QueryFavoriteRepository favoriteRepository;
    private final QueryRepository queryRepository;
    private final QueryService queryService;

    /**
     * Get all favorite queries for user
     */
    @Transactional(readOnly = true)
    public List<QueryResponse> getFavorites(User user) {
        log.info("Fetching favorites for user: {}", user.getEmail());

        List<QueryFavorite> favorites = favoriteRepository.findByUserOrderByCreatedAtDesc(user);

        return favorites.stream()
                .map(fav -> queryService.getQueryById(fav.getQuery().getId(), user))
                .collect(Collectors.toList());
    }

    /**
     * Add query to favorites
     */
    @Transactional
    public void addFavorite(UUID queryId, User user) {
        log.info("Adding query {} to favorites for user: {}", queryId, user.getEmail());

        Query query = queryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        // Check if already favorited
        if (favoriteRepository.existsByUserAndQuery(user, query)) {
            log.warn("Query {} is already favorited by user {}", queryId, user.getEmail());
            return;
        }

        QueryFavorite favorite = QueryFavorite.builder()
                .user(user)
                .query(query)
                .build();

        favoriteRepository.save(favorite);
        log.info("Query {} added to favorites", queryId);
    }

    /**
     * Remove query from favorites
     */
    @Transactional
    public void removeFavorite(UUID queryId, User user) {
        log.info("Removing query {} from favorites for user: {}", queryId, user.getEmail());

        Query query = queryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        favoriteRepository.deleteByUserAndQuery(user, query);
        log.info("Query {} removed from favorites", queryId);
    }

    /**
     * Check if query is favorited
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(UUID queryId, User user) {
        Query query = queryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        return favoriteRepository.existsByUserAndQuery(user, query);
    }
}