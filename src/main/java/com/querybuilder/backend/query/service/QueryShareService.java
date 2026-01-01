package com.querybuilder.backend.query.service;

import com.querybuilder.backend.auth.model.User;
import com.querybuilder.backend.auth.repository.UserRepository;
import com.querybuilder.backend.query.dto.ShareQueryRequest;
import com.querybuilder.backend.query.model.Query;
import com.querybuilder.backend.query.model.QueryShare;
import com.querybuilder.backend.query.repository.QueryRepository;
import com.querybuilder.backend.query.repository.QueryShareRepository;
import com.querybuilder.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for sharing queries with other users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryShareService {

    private final QueryShareRepository shareRepository;
    private final QueryRepository queryRepository;
    private final UserRepository userRepository;

    /**
     * Share query with user or make public
     */
    @Transactional
    public void shareQuery(UUID queryId, ShareQueryRequest request, User owner) {
        log.info("Sharing query {} by user: {}", queryId, owner.getEmail());

        Query query = queryRepository.findByIdAndUserAndDeletedFalse(queryId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        User sharedWithUser = null;

        // If email provided, find user
        if (request.getUserEmail() != null) {
            sharedWithUser = userRepository.findByEmail(request.getUserEmail())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "email", request.getUserEmail()));

            // Check if already shared
            if (shareRepository.existsByQueryAndSharedWithUser(query, sharedWithUser)) {
                log.warn("Query {} is already shared with user {}", queryId, request.getUserEmail());
                return;
            }
        }

        QueryShare share = QueryShare.builder()
                .query(query)
                .sharedWithUser(sharedWithUser)  // NULL = public
                .permission(request.getPermission())
                .sharedBy(owner)
                .build();

        shareRepository.save(share);

        if (sharedWithUser != null) {
            log.info("Query {} shared with user {}", queryId, sharedWithUser.getEmail());
        } else {
            log.info("Query {} made public", queryId);
        }
    }

    /**
     * Remove share (unshare)
     */
    @Transactional
    public void unshareQuery(UUID queryId, String userEmail, User owner) {
        log.info("Unsharing query {} from user {}", queryId, userEmail);

        Query query = queryRepository.findByIdAndUserAndDeletedFalse(queryId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        if (userEmail != null) {
            User sharedWithUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

            shareRepository.deleteByQueryAndSharedWithUser(query, sharedWithUser);
        }

        log.info("Query {} unshared", queryId);
    }

    /**
     * Get all shares for a query
     */
    @Transactional(readOnly = true)
    public List<QueryShare> getQueryShares(UUID queryId, User owner) {
        log.info("Fetching shares for query: {}", queryId);

        Query query = queryRepository.findByIdAndUserAndDeletedFalse(queryId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Query", "id", queryId));

        return shareRepository.findByQuery(query);
    }

    /**
     * Get queries shared with user
     */
    @Transactional(readOnly = true)
    public List<QueryShare> getSharedWithMe(User user) {
        log.info("Fetching queries shared with user: {}", user.getEmail());

        return shareRepository.findBySharedWithUser(user);
    }
}