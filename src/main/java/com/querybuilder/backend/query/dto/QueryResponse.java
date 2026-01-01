package com.querybuilder.backend.query.dto;

import com.querybuilder.backend.query.model.SharePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String sqlQuery;
    private String visualConfig;

    private DataSourceInfo dataSource;
    private UserInfo owner;

    private Boolean isTemplate;
    private Boolean isPublic;
    private Boolean isFavorite;
    private String category;
    private List<String> tags;
    private Integer version;

    private SharePermission currentUserPermission;  // If shared

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceInfo {
        private String id;
        private String name;
        private String databaseType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String name;
        private String email;
    }
}