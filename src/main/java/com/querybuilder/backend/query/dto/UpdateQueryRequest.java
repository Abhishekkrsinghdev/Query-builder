package com.querybuilder.backend.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQueryRequest {

    private String name;
    private String description;
    private String sqlQuery;
    private String visualConfig;
    private Boolean isTemplate;
    private Boolean isPublic;
    private String category;
    private List<String> tags;
    private String changeSummary;  // For versioning
}